package com.itheima.fve.fve.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.fve.fve.common.R;
import com.itheima.fve.fve.dto.DishDto; // 这个 import 在 page 方法中未使用，但其他方法可能需要
import com.itheima.fve.fve.dto.SetmealDto;
import com.itheima.fve.fve.entity.*;
import com.itheima.fve.fve.service.CategoryService;
import com.itheima.fve.fve.service.DishService; // 这个 import 在 page 方法中未使用，但其他方法可能需要
import com.itheima.fve.fve.service.SetmealDishService;
import com.itheima.fve.fve.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils; // 引入 StringUtils 用于更健壮的判断

import java.util.List;
import java.util.stream.Collectors;


@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService; // 这个注入在 page 方法中未使用，但其他方法可能需要

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService; // 这个注入在 page 方法中未使用，但其他方法可能需要


    /**
     * 新增套餐
     *
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询 - 返回包含分类名称的 DTO
     *
     * @param page
     * @param pageSize
     * @param name
     * @return R<Page<SetmealDto>>
     */
    @GetMapping("/page")
    // 修改返回类型为 R<Page<SetmealDto>>
    public R<Page<SetmealDto>> page(int page, int pageSize, String name) {
        log.info("套餐分页查询(DTO): page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 构造分页构造器对象 (用于查询 Setmeal 实体)
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        // 构造 DTO 分页构造器 (用于最终返回)
        Page<SetmealDto> dtoPage = new Page<>(page, pageSize);

        // 条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 添加名称模糊查询过滤器 (使用 StringUtils 更安全)
        queryWrapper.like(StringUtils.hasText(name), Setmeal::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 执行分页查询，结果填充到 pageInfo (Setmeal 列表)
        setmealService.page(pageInfo, queryWrapper);

        // 复制基础分页信息（总数、当前页等）到 dtoPage，但不包括记录列表 "records"
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");

        // 获取查询到的 Setmeal 实体列表
        List<Setmeal> records = pageInfo.getRecords();

        // 使用 Stream API 将 List<Setmeal> 转换为 List<SetmealDto>
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            // 复制 Setmeal 的属性到 SetmealDto
            BeanUtils.copyProperties(item, setmealDto);

            // 获取分类 ID
            Long categoryId = item.getCategoryId();
            // 根据分类 ID 查询分类对象
            if (categoryId != null) {
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    // 如果查到分类，设置 categoryName
                    setmealDto.setCategoryName(category.getName());
                } else {
                    // 如果根据 ID 没查到分类（可能数据不一致或分类被删）
                    log.warn("套餐 {} (ID: {}) 关联的分类 ID {} 未找到对应的分类记录", item.getName(), item.getId(), categoryId);
                    setmealDto.setCategoryName("分类丢失"); // 设置一个默认值或提示
                }
            } else {
                // 如果套餐没有设置分类 ID
                setmealDto.setCategoryName("未分类"); // 设置一个默认值
            }
            return setmealDto; // 返回填充好的 DTO 对象
        }).collect(Collectors.toList()); // 收集成 List<SetmealDto>

        // 将转换后的 DTO 列表设置到 dtoPage 中
        dtoPage.setRecords(list);

        // 返回包含 SetmealDto 列表的分页对象
        return R.success(dtoPage); //
    }

    /**
     * 套餐批量删除和单个删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        log.info("要删除的套餐 IDs: {}", ids);
        // 删除套餐及其关联商品
        setmealService.removeWithDish(ids);
        // 清除相关缓存 (虽然你的方法已经有 CacheEvict, 但如果 removeWithDish 内部没有清除相关缓存，这里可能需要额外处理，但通常 @CacheEvict 足够)
        return R.success("套餐删除成功");
    }

    /**
     * 批量修改套餐状态（启售/停售）
     * @param status 目标状态 (路径变量)
     * @param ids    套餐 ID 列表 (请求参数)
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "setmealCache", allEntries = true) // 状态变更也需要清除缓存
    public R<String> status(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        log.info("批量修改套餐状态: status = {}, ids = {}", status, ids);
        setmealService.updateStatus(status, ids);
        return R.success("套餐售卖状态修改成功");
    }

    /**
     * 根据条件查询套餐信息 (通常用于客户端，如按分类查询)
     *
     * @param setmeal 包含查询条件的 Setmeal 对象 (如 categoryId, status)
     * @return R<List<Setmeal>> (注意: 这里返回的是 Setmeal 列表，如果客户端也需要分类名，则需要修改)
     */
    @GetMapping("/list")
    // 使用 SpEL 动态生成缓存 key
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status", condition = "#setmeal.categoryId != null and #setmeal.status != null")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        log.info("根据条件查询套餐列表: categoryId={}, status={}", setmeal.getCategoryId(), setmeal.getStatus());
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus()); // 通常查启售状态 status=1
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }


    /**
     * 移动端点击套餐图片查看套餐具体内容
     * 返回套餐包含的商品信息 DTO 列表
     *
     * @param setmealId 套餐 ID (路径变量)
     * @return R<List<DishDto>>
     */
    @GetMapping("/dish/{id}")
    // 注意：这个接口通常不需要缓存，或者缓存粒度要细化，因为它依赖 SetmealDish 和 Dish 两张表的数据
    public R<List<DishDto>> dish(@PathVariable("id") Long setmealId) { // 参数名建议与路径变量一致
        log.info("查询套餐 {} 包含的商品详情", setmealId);
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealId);

        // 1. 查询套餐与商品的关联记录 (SetmealDish)
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);

        // 2. 遍历关联记录，查询对应的商品信息 (Dish)，并组装成 DishDto
        List<DishDto> dishDtos = setmealDishes.stream().map((setmealDish) -> {
                    DishDto dishDto = new DishDto();

                    // 复制 SetmealDish 中的属性到 DishDto (比如份数 copies)
                    // 注意：SetmealDish 和 DishDto 之间可拷贝的属性可能不多，主要是 copies
                    if (setmealDish.getCopies() != null) { // 假设 DishDto 有 setCopies 方法
                        // dishDto.setCopies(setmealDish.getCopies()); // 如果 DishDto 需要份数
                    }

                    // 获取关联的商品 ID
                    Long dishId = setmealDish.getDishId();
                    if (dishId != null) {
                        // 根据商品 ID 查询商品基本信息 (Dish)
                        Dish dish = dishService.getById(dishId);
                        if (dish != null) {
                            // 将查询到的 Dish 属性复制到 DishDto
                            // 这里会覆盖 dishDto 中可能已有的同名属性，但通常是期望的行为
                            BeanUtils.copyProperties(dish, dishDto);

                            // 可能还需要手动设置 SetmealDish 中的份数到 DishDto (如果 BeanUtils 没处理)
                            dishDto.setCopies(setmealDish.getCopies()); // 确保份数被设置

                        } else {
                            log.warn("套餐 {} (ID: {}) 关联的商品 ID {} 未找到对应的商品记录", setmealId, setmealDish.getId(), dishId);
                            // 可以选择返回一个包含错误信息的 DTO 或直接跳过这个商品
                            return null; // 返回 null，后续需要过滤掉
                        }
                    } else {
                        log.warn("套餐 {} (ID: {}) 的关联记录 SetmealDish (ID: {}) 中的 dishId 为空", setmealId, setmealDish.getId());
                        return null; // 返回 null
                    }

                    return dishDto;
                })
                .filter(dto -> dto != null) // 过滤掉处理过程中产生的 null 值
                .collect(Collectors.toList());

        return R.success(dishDtos);
    }
    // --- 添加这个方法来处理更新套餐的 PUT 请求 ---
    /**
     * 更新套餐信息 (包含商品)
     *
     * @param setmealDto 包含更新信息的 DTO 对象
     * @return
     */
    @PutMapping // 使用 @PutMapping 注解来处理 HTTP PUT 请求
    @CacheEvict(value = "setmealCache", allEntries = true) // 更新操作同样需要清除缓存
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        // 记录日志，记录接收到的数据
        log.info("更新套餐信息: {}", setmealDto);

        // 检查 ID 是否存在，更新操作必须有 ID
        if (setmealDto.getId() == null) {
            return R.error("更新套餐失败，套餐ID不能为空");
        }

        try {
            // 调用 Service 层中的 updateWithDish 方法来执行更新逻辑
            setmealService.updateWithDish(setmealDto);
            // 返回成功响应
            return R.success("套餐修改成功");
        } catch (Exception e) {
            // 如果 Service 层或其他地方抛出异常，记录错误日志并返回错误信息
            log.error("更新套餐失败", e);
            return R.error("更新套餐失败：" + e.getMessage());
        }
    }
    /**
     * 根据 ID 查询套餐信息和对应的商品信息
     * @param id 套餐的 ID
     * @return R<SetmealDto> 包含套餐和商品列表的 DTO
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id) {

         log.info(">>> 进入 SetmealController.getById 方法, 接收到 ID: {}", id);
        log.info("根据ID查询套餐信息: {}", id);
        // 确认这里调用的是 setmealService，并且方法存在于 Service 实现中
        SetmealDto setmealDto = setmealService.getByIdWithDishes(id);
        if (setmealDto != null) {
            return R.success(setmealDto);
        } else {
            return R.error("没有查询到对应套餐信息");
        }

    }
}