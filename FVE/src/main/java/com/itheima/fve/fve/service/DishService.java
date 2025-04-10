package com.itheima.fve.fve.service;

import com.baomidou.mybatisplus.extension.service.IService;
import  com.itheima.fve.fve.entity.Dish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
public interface DishService extends IService<Dish> {
}
