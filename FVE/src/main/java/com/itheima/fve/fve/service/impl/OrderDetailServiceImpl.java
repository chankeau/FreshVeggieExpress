package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.CustomException;
import com.itheima.fve.fve.entity.AddressBook;
import com.itheima.fve.fve.entity.OrderDetail;
import com.itheima.fve.fve.entity.Orders;
import com.itheima.fve.fve.entity.ShoppingCart;
import com.itheima.fve.fve.mapper.OrderDetailMapper;
import com.itheima.fve.fve.service.AddressBookService;
import com.itheima.fve.fve.service.OrderDetailService;
import com.itheima.fve.fve.service.ShoppingCartService;
import com.itheima.fve.fve.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {

}