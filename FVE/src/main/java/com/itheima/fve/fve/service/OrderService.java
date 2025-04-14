package com.itheima.fve.fve.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.fve.fve.entity.Orders;

public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     * @param orders
     */
    public void submit(Orders orders);

    /**
     * 用户再来一单
     * @param id
     */
    public void orderAgain(long id);
}
