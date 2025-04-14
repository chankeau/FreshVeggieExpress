package com.itheima.fve.fve.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.R;
import com.itheima.fve.fve.dto.OrdersDto;
import com.itheima.fve.fve.entity.OrderDetail;
import com.itheima.fve.fve.entity.Orders;
import com.itheima.fve.fve.service.OrderDetailService;
import com.itheima.fve.fve.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@Slf4j
@RequestMapping("/order")
public class OrderDetailController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;


    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize,String number,String beginTime,String endTime){
        log.info("page={}, pageSize={}, number={}, beginTime={}, endTime={}",page, pageSize, number, beginTime, endTime);
        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(number!=null,Orders::getNumber,number)
                .ge(StringUtils.isNotEmpty(beginTime),Orders::getOrderTime,beginTime)
                .le(StringUtils.isNotEmpty(endTime),Orders::getOrderTime,endTime);

        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);

        //执行查询
        orderService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 更新状态
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Orders orders){
        //判断传入参数是否合法
        Long orderId = orders.getId();
        Integer status = orders.getStatus();

        if(orderId == null || status==null){
            return R.error("传入信息不合法");
        }

        log.info("订单数据{}",orders);
        //SQL: UPDATE orders SET status=? WHERE id=?
        orderService.updateById(orders);
        return R.success("订单修改状态成功");
    }




    /**
     * 客户端点击再来一单
     * @param map
     * @return
     */
    @PostMapping("/again")
    /**
     * 前端点击再来一单是直接跳转到购物车的，所以为了避免数据有问题，再跳转之前我们需要把购物车的数据给清除
     * ①通过orderId获取订单明细
     * ②把订单明细的数据的数据塞到购物车表中，不过在此之前要先把购物车表中的数据给清除(清除的是当前登录用户的购物车表中的数据)，
     * 不然就会导致再来一单的数据有问题；
     * (这样可能会影响用户体验，但是对于外卖来说，用户体验的影响不是很大，电商项目就不能这么干了)
     */
    public R<String> again(@RequestBody Map<String,String> map){
        log.info("订单id:{}",map);
        String ids = map.get("id");
        long id = Long.parseLong(ids);
        orderService.orderAgain(id);
        return R.success("再来一单");
    }

    //抽离的一个方法，通过订单id查询订单明细，得到一个订单明细的集合
    //这里抽离出来是为了避免在stream中遍历的时候直接使用构造条件来查询导致eq叠加，从而导致后面查询的数据都是null
    public List<OrderDetail> getOrderDetailListByOrderId(Long orderId){
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);
        return orderDetailList;
    }
//    /**
//     * 查询用户订购过的订单
//     * @param page
//     * @param pageSize
//     * @return
//     */
//    @GetMapping("/userPage")
//    public R<Page> userPage(int page, int pageSize){
//        log.info("page={}, pageSize={}",page, pageSize);
//        Page<Orders> pageInfo = new Page<>(page, pageSize);
//        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.orderByDesc(Orders::getOrderTime);
//        orderService.page(pageInfo,queryWrapper);
//        return R.success(pageInfo);
//    }

    /**
     * 用户端展示自己的订单分页查询
     * 遇到的坑：原来分页对象中的records集合存储的对象是分页泛型中的对象，里面有分页泛型对象的数据
     * 开始的时候我以为前端只传过来了分页数据，其他所有的数据都要从本地线程存储的用户id开始查询，
     * 结果就出现了一个用户id查询到 n个订单对象，然后又使用 n个订单对象又去查询 m 个订单明细对象，
     * 结果就出现了评论区老哥出现的bug(嵌套显示数据....)
     * 正确方法:直接从分页对象中获取订单id就行，问题大大简化了......
     * @param page
     * @param pageSize
     * @return

     */
    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize){
        //分页构造器对象
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> pageDto = new Page<>(page,pageSize);
        //构造条件查询对象
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        //这里是直接把当前用户分页的全部结果查询出来，要添加用户id作为查询条件，否则会出现用户可以查询到其他用户的订单情况
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo,queryWrapper);

        //通过OrderId查询对应的OrderDetail
        LambdaQueryWrapper<OrderDetail> queryWrapper2 = new LambdaQueryWrapper<>();

        //对OrderDto进行需要的属性赋值
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> orderDtoList = records.stream().map((item) ->{
            OrdersDto orderDto = new OrdersDto();
            //此时的orderDto对象里面orderDetails属性还是空 下面准备为它赋值
            Long orderId = item.getId();//获取订单id
            List<OrderDetail> orderDetailList = this.getOrderDetailListByOrderId(orderId);
            BeanUtils.copyProperties(item,orderDto);
            //对orderDto进行OrderDetails属性的赋值
            orderDto.setOrderDetails(orderDetailList);
            return orderDto;
        }).collect(Collectors.toList());

        //使用dto的分页有点难度.....需要重点掌握
        BeanUtils.copyProperties(pageInfo,pageDto,"records");
        pageDto.setRecords(orderDtoList);
        return R.success(pageDto);
    }
}
