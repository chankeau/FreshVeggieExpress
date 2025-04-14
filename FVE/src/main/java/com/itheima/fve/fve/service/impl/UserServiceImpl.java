package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.entity.User;
import com.itheima.fve.fve.mapper.UserMapper;
import com.itheima.fve.fve.service.UserService;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
