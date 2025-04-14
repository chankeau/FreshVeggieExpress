package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.entity.ShoppingCart;
import com.itheima.fve.fve.mapper.ShoppingCartMapper;
import com.itheima.fve.fve.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Override
    public void clean() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null) {
            LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ShoppingCart::getUserId, currentUserId);
            this.remove(queryWrapper);
        } else {
            log.warn("尝试清空购物车但无法获取用户ID");
            // 或者根据业务抛出异常
        }
    }
}
