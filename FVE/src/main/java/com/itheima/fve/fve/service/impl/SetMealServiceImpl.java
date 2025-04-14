package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 引入 Mybatis-Plus 的 Page
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.common.CustomException;
import com.itheima.fve.fve.dto.SetmealDto;
import com.itheima.fve.fve.entity.Category; // 引入 Category 实体
import com.itheima.fve.fve.entity.Setmeal;
import com.itheima.fve.fve.entity.SetmealDish;
import com.itheima.fve.fve.mapper.SetmealMapper;
import com.itheima.fve.fve.service.CategoryService; // 引入 CategoryService
import com.itheima.fve.fve.service.SetmealDishService;
import com.itheima.fve.fve.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils; // 引入 BeanUtils
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // 引入 StringUtils

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SetMealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    // 不再需要注入 SetmealService 自身
    // @Autowired
    // private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService; // 注入 CategoryService

    /**
     * 新增套餐，同时需要保存套餐和商品关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐基本信息，操作setmeal，执行insert操作
        this.save(setmealDto); // 使用 this.save()

        //保存套餐和商品的关联信息，操作setmeal_dish，执行insert操作
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //检查setmealDishes是否为空
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes = setmealDishes.stream().map((item) ->{
                item.setSetmealId(setmealDto.getId()); // 获取保存后生成的 setmealId
                return item;
            }).collect(Collectors.toList());
            setmealDishService.saveBatch(setmealDishes);
        } else {
            log.warn("新增套餐 {} 时未提供关联商品信息", setmealDto.getName());
        }
    }

    /**
     * 批量删除套餐和关联数据
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //构造条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //先查询该套餐是否在售卖，如果是则抛出业务异常
        queryWrapper.in(ids != null && !ids.isEmpty(), Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus,1);

        long count = this.count(queryWrapper);
        // 如果不能删除，抛出业务异常
        if(count > 0) {
            throw new CustomException("删除套餐中有正在售卖的套餐,无法全部删除");
        }
        // 如果可以删除，先删除套餐表中的数据
        this.removeByIds(ids);
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(lambdaQueryWrapper);
    }

    /**
     * 批量停售/启售套餐
     * @param status 目标状态 0:停售 1:启售
     * @param ids 套餐ID列表
     */
    @Override
    @Transactional // 批量更新建议加上事务
    public void updateStatus(Integer status, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("批量更新套餐状态时传入的ID列表为空");
            return; // 或者抛出异常，取决于业务要求
        }
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        //根据 ID 列表进行批量查询
        List<Setmeal> list = this.list(queryWrapper); // 使用 this.list()

        for (Setmeal setmeal : list) {
            if (setmeal != null){
                setmeal.setStatus(status);
                this.updateById(setmeal); // 使用 this.updateById()
            }
        }
        /* 或者，更高效的批量更新方式 (如果数据库中不存在对应ID，也不会报错)
        Setmeal setmealUpdate = new Setmeal();
        setmealUpdate.setStatus(status);
        LambdaQueryWrapper<Setmeal> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.in(Setmeal::getId, ids);
        this.update(setmealUpdate, updateWrapper); // 使用 this.update()
        */
    }

    /**
     * 分页查询套餐信息，并关联查询分类名称
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 套餐名称 (用于模糊查询)
     * @return Page<SetmealDto> 包含分类名称的分页结果
     */
    @Override // 确保 SetmealService 接口中有此方法的声明
    public Page<SetmealDto> getDtoPage(int page, int pageSize, String name) {
        // 1. 创建 Setmeal 实体的分页对象
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>(); // 用于最终返回的 DTO 分页对象

        // 2. 设置查询条件
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 添加名称模糊查询条件
        queryWrapper.like(StringUtils.hasText(name), Setmeal::getName, name);
        // 添加排序条件，例如按更新时间降序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 3. 执行分页查询 (基于 Setmeal 实体)
        this.page(setmealPage, queryWrapper); // 使用 this.page()

        // 4. 复制分页信息 (总记录数、总页数等), 但不复制 records 列表
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");

        // 5. 处理查询到的 Setmeal 记录列表
        List<Setmeal> records = setmealPage.getRecords();

        List<SetmealDto> dtoList = records.stream().map(setmeal -> {
            // 5.1 创建 SetmealDto 对象
            SetmealDto setmealDto = new SetmealDto();
            // 5.2 复制 Setmeal 的基本属性到 SetmealDto
            BeanUtils.copyProperties(setmeal, setmealDto);

            // 5.3 获取 categoryId
            Long categoryId = setmeal.getCategoryId();

            // 5.4 根据 categoryId 查询 Category 名称 【核心步骤】
            if (categoryId != null) {
                // 调用 CategoryService 查询分类信息
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    // 5.5 设置 categoryName
                    setmealDto.setCategoryName(category.getName());
                } else {
                    // 处理分类不存在的情况
                    log.warn("套餐 {} (ID: {}) 关联的分类 ID {} 不存在", setmeal.getName(), setmeal.getId(), categoryId);
                    setmealDto.setCategoryName("分类已删除"); // 或者设置为 null 或其他默认值
                }
            } else {
                // 处理 categoryId 为空的情况
                setmealDto.setCategoryName("未分类");
            }

            // 返回处理好的 DTO 对象
            return setmealDto;
        }).collect(Collectors.toList()); // 收集成 List<SetmealDto>

        // 6. 将处理后的 DTO 列表设置到 DTO 分页对象中
        setmealDtoPage.setRecords(dtoList);

        // 7. 返回 DTO 分页对象
        return setmealDtoPage;
    }
    /**
     * 根据ID获取套餐信息和对应的商品信息
     * @param id 套餐ID
     * @return SetmealDto 包含套餐和商品列表的 DTO，如果找不到则返回 null
     */
    @Override // 记得加上 @Override 注解
    public SetmealDto getByIdWithDishes(Long id) {
        // 1. 查询套餐基本信息 (setmeal 表)
        Setmeal setmeal = this.getById(id); // 使用 ServiceImpl 自带的 getById 方法

        if (setmeal == null) {
            // 如果根据 ID 没查到套餐，直接返回 null
            log.warn("根据 ID {} 未查询到 Setmeal 记录", id);
            return null;
        }

        // 2. 查询当前套餐对应的商品信息 (setmeal_dish 表)
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        // 查询条件：setmeal_id 等于传入的 id (这里的 setmealId 是 SetmealDish 实体中的字段)
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        // 通过 SetmealDishService 查询关联的商品列表
        List<SetmealDish> dishes = setmealDishService.list(queryWrapper);

        // 3. 将查询到的 setmeal 信息和 dishes 列表组装成 SetmealDto 对象
        SetmealDto setmealDto = new SetmealDto();
        // 复制 setmeal 的属性到 setmealDto
        BeanUtils.copyProperties(setmeal, setmealDto);
        // 设置商品列表到 DTO 中
        setmealDto.setSetmealDishes(dishes);

        // 4. 返回组装好的 DTO 对象
        return setmealDto;
    }
    // --- 添加结束 ---

    // (保留你其他的已有方法: saveWithDish, removeWithDish, updateStatus, getDtoPage ...)

    // --- 另外，你也需要实现 updateWithDish ---
    /**
     * 更新套餐信息，同时更新套餐和商品的关联关系
     * Controller 中处理 PUT 请求时会用到这个方法 (如果你添加了 @PutMapping 接口)
     * @param setmealDto
     */
    @Override // 如果接口中有声明，这里也要实现
    @Transactional // 保证操作原子性
    public void updateWithDish(SetmealDto setmealDto) {
        // 1. 更新套餐基本信息 (setmeal 表)
        this.updateById(setmealDto); // 使用 mybatis-plus 提供的 updateById 更新 Setmeal

        // 2. 删除该套餐原有关联的商品 (setmeal_dish 表)
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper); // 通过 SetmealDishService 删除

        // 3. 获取 DTO 中新的商品列表
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        // 4. 给新的商品列表设置正确的 setmealId
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes = setmealDishes.stream().map((item) -> {
                item.setSetmealId(setmealDto.getId()); // 设置当前套餐的 ID
                return item;
            }).collect(Collectors.toList());

            // 5. 批量保存新的关联关系
            setmealDishService.saveBatch(setmealDishes); // 通过 SetmealDishService 批量保存
        }
        log.info("成功更新套餐 ID: {}", setmealDto.getId());
    }
}