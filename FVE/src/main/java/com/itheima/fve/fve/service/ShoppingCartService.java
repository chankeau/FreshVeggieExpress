package com.itheima.fve.fve.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.fve.fve.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    /**
     * 清理购物车
     */
    public void clean();
}
