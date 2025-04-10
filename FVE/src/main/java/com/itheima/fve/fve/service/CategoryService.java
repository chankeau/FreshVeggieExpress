package com.itheima.fve.fve.service;

import com.baomidou.mybatisplus.extension.service.IService;
import  com.itheima.fve.fve.entity.Category;


public interface CategoryService extends IService<Category> {
    public void remove(Long id);
}
