package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.CustomException;
import com.itheima.fve.fve.entity.*;
import com.itheima.fve.fve.mapper.OrderMapper;
import com.itheima.fve.fve.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private OrderDetailService orderDetailService;

    @Override
    @Transactional
    public void submit(Orders orders) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new CustomException("用户未登录");
        }

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);

        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            throw new CustomException("购物车为空，无法下单");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new CustomException("用户信息获取失败");
        }

        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误，无法下单");
        }
        if (!addressBook.getUserId().equals(userId)) {
            throw new CustomException("地址信息与用户不匹配");
        }

        long orderId = IdWorker.getId();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setQuantity(item.getNumber());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            return orderDetail;
        }).collect(Collectors.toList());

        totalAmount = orderDetails.stream()
                .map(detail -> detail.getAmount().multiply(new BigDecimal(detail.getQuantity()))) // Uses getQuantity() from OrderDetail
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        orders.setNumber(String.valueOf(orderId));
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(totalAmount);
        orders.setUserId(userId);
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        this.save(orders);
        orderDetailService.saveBatch(orderDetails);

        LambdaQueryWrapper<ShoppingCart> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(ShoppingCart::getUserId, userId);
        shoppingCartService.remove(removeWrapper);
    }

    @Override
    @Transactional
    public void orderAgain(long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new CustomException("用户未登录");
        }

        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);

        if(orderDetailList == null || orderDetailList.isEmpty()){
            throw new CustomException("找不到原始订单明细");
        }

        LambdaQueryWrapper<ShoppingCart> cleanWrapper = new LambdaQueryWrapper<>();
        cleanWrapper.eq(ShoppingCart::getUserId, userId);
        shoppingCartService.remove(cleanWrapper);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            shoppingCart.setImage(item.getImage());
            Long dishId = item.getDishId();
            Long setmealId = item.getSetmealId();
            if (dishId != null) {
                shoppingCart.setDishId(dishId);
            } else {
                shoppingCart.setSetmealId(setmealId);
            }
            shoppingCart.setName(item.getName());
            shoppingCart.setNumber(item.getQuantity());
            shoppingCart.setAmount(item.getAmount());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartService.saveBatch(shoppingCartList);
    }
}