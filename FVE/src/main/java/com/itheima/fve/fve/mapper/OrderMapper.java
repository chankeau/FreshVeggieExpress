package com.itheima.fve.fve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.fve.fve.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
    public void submit(Orders orders);
}