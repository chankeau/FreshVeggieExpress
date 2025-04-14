package com.itheima.fve.fve.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.R;
import com.itheima.fve.fve.entity.ShoppingCart;
import com.itheima.fve.fve.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // 引入 Map

@RequestMapping("/shoppingCart")
@Slf4j
@RestController
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    // add 方法保持不变...
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据：{}", shoppingCart);

        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            return R.error("用户未登录");
        }
        shoppingCart.setUserId(currentId);

        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);

        if (dishId != null) {
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else if (setmealId != null) {
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        } else {
            return R.error("参数错误，未指定菜品或套餐");
        }

        ShoppingCart serviceOne = shoppingCartService.getOne(queryWrapper);

        if (serviceOne != null) {
            Integer number = serviceOne.getNumber();
            serviceOne.setNumber(number + 1);
            shoppingCartService.updateById(serviceOne);
        } else {
            // 确保插入时有 amount (从 dish/setmeal 表获取或前端传递，这里假设前端已传)
            if (shoppingCart.getAmount() == null) {
                // 这里应该有逻辑从 dishService 或 setmealService 获取价格
                log.warn("添加新购物车项时缺少金额，可能导致后续计算错误。DishId: {}, SetmealId: {}", dishId, setmealId);
                // return R.error("无法获取商品价格"); // 或者允许插入，但后续计算总价会出错
            }
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            serviceOne = shoppingCart;
        }

        return R.success(serviceOne);
    }


    /**
     * 减少购物车商品数量 或 移除商品
     * 前端应发送 { "dishId": xxx } 或 { "setmealId": xxx }
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody Map<String, Long> payload) { // 使用 Map 接收，更灵活
        Long dishId = payload.get("dishId");
        Long setmealId = payload.get("setmealId");
        log.info("请求减少购物车项: dishId={}, setmealId={}", dishId, setmealId);

        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            return R.error("用户未登录");
        }

        // 校验参数
        if (dishId == null && setmealId == null) {
            return R.error("参数错误，未指定菜品或套餐");
        }

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);
        // 根据传入的 ID 构建查询条件
        if (dishId != null) {
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }

        ShoppingCart cartItem = shoppingCartService.getOne(queryWrapper);

        if (cartItem != null) {
            Integer currentNumber = cartItem.getNumber();
            if (currentNumber > 1) {
                // 数量大于1，减1并更新
                cartItem.setNumber(currentNumber - 1);
                shoppingCartService.updateById(cartItem);
                log.info("购物车项 ID: {} 数量减1成功，新数量: {}", cartItem.getId(), cartItem.getNumber());
                return R.success(cartItem); // 返回更新后的对象
            } else {
                // 数量等于1或小于等于0（异常情况），直接移除
                shoppingCartService.removeById(cartItem.getId());
                log.info("购物车项 ID: {} 已移除", cartItem.getId());
                // *** 返回一个 number 为 0 的对象，方便前端处理 ***
                // 我们可以克隆原始对象，然后修改数量，或者创建一个新的包含必要信息和数量0的对象
                ShoppingCart removedItemInfo = new ShoppingCart();
                removedItemInfo.setId(cartItem.getId()); // 可能不需要ID，但带着方便
                removedItemInfo.setDishId(cartItem.getDishId());
                removedItemInfo.setSetmealId(cartItem.getSetmealId());
                removedItemInfo.setNumber(0); // 关键：数量为0
                // removedItemInfo.setName(cartItem.getName()); // 其他信息可选添加
                return R.success(removedItemInfo);
            }
        } else {
            // 数据库中没有找到对应的购物车项
            log.warn("尝试减少不存在的购物车商品: userId={}, dishId={}, setmealId={}", currentId, dishId, setmealId);
            // 即使找不到，也返回一个数量为0的对象，让前端统一处理逻辑
            ShoppingCart notFoundItemInfo = new ShoppingCart();
            notFoundItemInfo.setDishId(dishId);
            notFoundItemInfo.setSetmealId(setmealId);
            notFoundItemInfo.setNumber(0);
            return R.success(notFoundItemInfo); // 或者返回 R.error("购物车中没有该商品")
        }
    }

    // list 方法保持不变...
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("查看购物车");
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return R.error("用户未登录");
        }
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentUserId);
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        log.info("购物车列表查询结果大小: {}", (list == null ? "null" : list.size()));
        return R.success(list);
    }

    // clean 方法保持不变...
    @DeleteMapping("/clean")
    public R<String> clean() {
        log.info("清空购物车");
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return R.error("用户未登录");
        }
        // 确认 service.clean() 的实现是安全的，只清空当前用户
        shoppingCartService.clean();
        return R.success("购物车已清空");
    }
}