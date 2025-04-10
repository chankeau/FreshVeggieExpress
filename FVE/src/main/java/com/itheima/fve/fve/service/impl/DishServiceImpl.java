package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import  com.itheima.fve.fve.common.CustomException;
//import  com.itheima.fve.fve.dto.DishDto;
import  com.itheima.fve.fve.entity.Dish;
//import  com.itheima.fve.fve.entity.DishFlavor;
import  com.itheima.fve.fve.mapper.DishMapper;
//import  com.itheima.fve.fve.service.DishFlavorService;
import  com.itheima.fve.fve.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
}