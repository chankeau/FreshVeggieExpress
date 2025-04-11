package com.itheima.fve.fve.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.fve.fve.common.CustomException;
import com.itheima.fve.fve.common.R;
import com.itheima.fve.fve.dto.DishDto;
import com.itheima.fve.fve.entity.Category;
import com.itheima.fve.fve.entity.Dish;
import com.itheima.fve.fve.service.CategoryService;
import com.itheima.fve.fve.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService; // 依赖 DishService


    @Autowired
    private CategoryService categoryService;



    /**
     * 新增菜品
     *
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("接收到新增菜品数据：{}", dishDto);
        try {
            dishService.saveDish(dishDto);


//            String key = "dish_" + dishDto.getCategoryId() + "_1"; // 假设清理启售状态缓存
//            redisTemplate.delete(key);
//            log.info("清理了可能的Redis缓存: {}", key);

            return R.success("新增菜品成功");
        } catch (Exception e) {
            log.error("新增菜品失败", e);
            return R.error("新增菜品失败：" + e.getMessage());
        }
    }

    /**
     * 菜品信息分页查询
     *
     * @param page
     * @param pageSize
     * @param name     可选的名称过滤条件
     * @return 返回 R<Page<DishDto>>
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        log.info("菜品分页查询: page={}, pageSize={}, name={}", page, pageSize, name);

        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null && !name.isEmpty(), Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        dishService.page(pageInfo, queryWrapper);

        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();
            if (categoryId != null) {
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    dishDto.setCategoryName(category.getName());
                }
            }
            // 此处不再需要查询和设置口味信息
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage); // <--- 修改点：返回 dishDtoPage
    }

    /**
     * 根据id查询菜品信息 (已移除口味处理)
     *
     * @param id 菜品ID
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {
        log.info("根据ID查询菜品信息: {}", id);
        // 调用 Service 层中不含口味逻辑的查询方法
        DishDto dishDto = dishService.getDishDtoById(id); // <--- 修改点
        if (dishDto == null) {
            return R.error("未找到对应菜品");
        }
        return R.success(dishDto);
    }

    /**
     * 更新菜品 (已移除口味处理)
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info("接收到更新菜品数据：{}", dishDto);
        if (dishDto.getId() == null) {
            return R.error("更新失败，缺少菜品ID");
        }
        try {
            // 调用 Service 层中不含口味逻辑的更新方法
            dishService.updateDish(dishDto); // <--- 修改点

            // 清理缓存
//            String key = "dish_" + dishDto.getCategoryId() + "_1";
//            redisTemplate.delete(key);
//            log.info("清理了可能的Redis缓存: {}", key);
            // 或者清理所有 dish_* 缓存
            // Set<String> keys = redisTemplate.keys("dish_*");
            // if (keys != null && !keys.isEmpty()) { redisTemplate.delete(keys); }

            return R.success("更新菜品成功");
        } catch (Exception e) {
            log.error("更新菜品失败", e);
            return R.error("更新菜品失败：" + e.getMessage());
        }
    }

    /**
     * 对菜品批量或单个进行停售或启售 (逻辑不变)
     * @param status 路径变量，0停售，1启售
     * @param ids    请求参数，菜品ID列表
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        if (status == null || (status != 0 && status != 1)) {
            return R.error("无效的状态值");
        }
        if (ids == null || ids.isEmpty()) {
            return R.error("请选择需要修改状态的菜品");
        }
        log.info("批量修改售卖状态 Status: {}, IDs: {}", status, ids);
        try {
            // 调用 Service 层更新状态的方法 (该方法本身不应涉及口味)
            dishService.updateStatus(status, ids);

            // 清理所有 dish_* 缓存，因为状态变化会影响列表查询结果
//            Set<String> keys = redisTemplate.keys("dish_*");
//            if (keys != null && !keys.isEmpty()) {
//                redisTemplate.delete(keys);
//                log.info("因状态变更，清理了Redis缓存: {}", keys);
//            }

            return R.success("售卖状态修改成功");
        } catch (Exception e) {
            log.error("修改售卖状态失败", e);
            return R.error("修改售卖状态失败：" + e.getMessage());
        }
    }

    /**
     * 菜品批量删除和单个删除 (已移除口味处理)
     * @param ids 请求参数，菜品ID列表
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.error("请选择需要删除的菜品");
        }
        log.info("准备删除菜品，IDs: {}", ids);
        try {
            // 调用 Service 层删除菜品的方法 (该方法不应涉及口味)
            dishService.deleteDishByIds(ids); // <--- 修改点：确保此方法只删 Dish

            // 移除删除菜品对应口味的逻辑
            /*
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(DishFlavor::getDishId, ids);
            dishFlavorService.remove(queryWrapper); // 移除此行
            */

            // 清理所有菜品的缓存数据
//            Set<String> keys = redisTemplate.keys("dish_*");
//            if (keys != null && !keys.isEmpty()) {
//                redisTemplate.delete(keys);
//                log.info("因删除操作，清理了Redis缓存: {}", keys);
//            }

            return R.success("菜品删除成功");
        } catch (CustomException e) {
            log.error("删除菜品业务校验失败", e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除菜品失败", e);
            return R.error("删除菜品失败，请稍后重试");
        }
    }

    /**
     * 根据条件查询对应菜品数据 (已移除口味处理)
     *
     * @param dish 查询条件，常用 categoryId 和 status
     * @return 返回 List<DishDto>，其中不包含口味信息
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        log.info("根据条件查询菜品列表: categoryId={}, status={}", dish.getCategoryId(), dish.getStatus());
        List<DishDto> dishDtoList = null;
        String key = null;

        // 优先从 Redis 获取缓存
        if (dish.getCategoryId() != null) {
            int queryStatus = (dish.getStatus() != null) ? dish.getStatus() : 1; // 默认查启售
            key = "dish_" + dish.getCategoryId() + "_" + queryStatus;
            log.debug("尝试从Redis获取缓存，key: {}", key);
//            try {
//                dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
//            } catch (Exception e) {
//                log.error("从 Redis 获取缓存失败, key: {}", key, e);
//            }
        }

        if (dishDtoList != null) {
            log.info("命中Redis缓存，key: {}", key);
            return R.success(dishDtoList);
        }

        log.info("Redis缓存未命中或未使用缓存，查询数据库...");
        // 构造数据库查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus, dish.getStatus() != null ? dish.getStatus() : 1); // 默认查启售
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        // 转换 Dish 列表为 DishDto 列表
        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            if (item.getCategoryId() != null) {
                Category category = categoryService.getById(item.getCategoryId());
                if (category != null) {
                    dishDto.setCategoryName(category.getName());
                }
            }

            // 移除查询和设置口味的逻辑
            /*
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper); // 移除此服务调用
            dishDto.setFlavors(dishFlavorList); // 移除设置
            */
            return dishDto;
        }).collect(Collectors.toList());

        // 将结果缓存到 Redis
        if (key != null && dishDtoList != null) { // 只有当 key 有效且查询结果不为 null 时才缓存
            log.info("将查询结果写入Redis缓存，key: {}, 大小: {}, 过期时间: 60分钟", key, dishDtoList.size());
//            try {
//                redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
//            } catch (Exception e) {
//                log.error("写入 Redis 缓存失败, key: {}", key, e);
//            }
        }

        return R.success(dishDtoList);
    }
}