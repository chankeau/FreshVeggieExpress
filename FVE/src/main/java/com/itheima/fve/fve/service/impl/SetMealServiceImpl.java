package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import  com.itheima.fve.fve.common.CustomException;
//import  com.itheima.fve.fve.dto.SetmealDto;
import  com.itheima.fve.fve.entity.Dish;
import  com.itheima.fve.fve.entity.Setmeal;
import  com.itheima.fve.fve.entity.SetmealDish;
import  com.itheima.fve.fve.mapper.SetmealMapper;
import com.itheima.fve.fve.service.SetmealDishService;
import  com.itheima.fve.fve.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SetMealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {


}
