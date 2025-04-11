package com.itheima.fve.fve.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
//import  com.itheima.fve.fve.dto.SetmealDto;
import com.itheima.fve.fve.dto.SetmealDto;
import  com.itheima.fve.fve.entity.Setmeal;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时需要保存套餐和菜品关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 批量删除套餐和关联数据
     * @param ids
     */
    public void removeWithDish(List<Long> ids);


    /**
     * 批量停售套餐
     * @param status
     * @param ids
     */
    public void updateStatus(Integer status, List<Long> ids);

    // 确保 SetmealService 接口中有此方法的声明
    Page<SetmealDto> getDtoPage(int page, int pageSize, String name);

    // 记得加上 @Override 注解
    SetmealDto getByIdWithDishes(Long id);

    // 如果接口中有声明，这里也要实现
    @Transactional
    // 保证操作原子性
    void updateWithDish(SetmealDto setmealDto);
}
