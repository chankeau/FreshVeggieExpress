package com.itheima.fve.fve.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.fve.fve.dto.DishDto;
import  com.itheima.fve.fve.entity.Dish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
public interface DishService extends IService<Dish> {
    void saveDish(DishDto dishDto);

    DishDto getDishDtoById(Long id);

    void updateDish(DishDto dishDto);

    void updateStatus(Integer status, List<Long> ids);

    void deleteDishByIds(List<Long> ids);
}
