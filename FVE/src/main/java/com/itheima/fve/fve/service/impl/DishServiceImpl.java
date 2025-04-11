package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.common.CustomException; // 确保这个自定义异常类存在
import com.itheima.fve.fve.dto.DishDto; // 引入 DishDto
import com.itheima.fve.fve.entity.Category; // 引入 Category
import com.itheima.fve.fve.entity.Dish;
// import com.itheima.fve.fve.entity.DishFlavor; // 已移除
import com.itheima.fve.fve.mapper.DishMapper;
import com.itheima.fve.fve.service.CategoryService; // 引入 CategoryService
import com.itheima.fve.fve.service.DishService;
// import com.itheima.fve.fve.service.DishFlavorService; // 已移除
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
// import java.util.stream.Collectors; // 如果不用 stream 操作 DTO 转换则不需要

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    // 注入 CategoryService 以便获取分类名称
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增产品
     * @param dishDto 包含产品基本信息和分类ID
     */
    @Override
    @Transactional // 开启事务
    public void saveDish(DishDto dishDto) {
        log.info("开始保存产品（无口味）: {}", dishDto.getName());
        // 1. 保存基本的产品信息到 dish 表
        Dish dish = new Dish();
        // 从 DishDto 复制属性到 Dish 实体 (name, price, code, image, description, status, categoryId 等)
        BeanUtils.copyProperties(dishDto, dish);

        // 使用 ServiceImpl 提供的 save 方法保存 Dish 实体
        boolean saveResult = this.save(dish);

        if (!saveResult) {
            log.error("保存产品基本信息失败: {}", dish.getName());
            throw new CustomException("新增产品失败，请稍后重试"); // 抛出自定义异常
        }
        log.info("产品基本信息保存成功, ID: {}", dish.getId()); // 保存后 dish 对象会有 ID

        // 2. 不再需要保存口味信息
    }

    /**
     * 根据ID获取产品信息和分类名称（不含口味）
     * @param id 产品ID
     * @return DishDto 或 null
     */
    @Override
    public DishDto getDishDtoById(Long id) {
        log.debug("根据ID查询产品 DTO（无口味），ID: {}", id);
        // 1. 查询基本的产品信息
        Dish dish = this.getById(id);

        if (dish == null) {
            log.warn("未找到 ID 为 {} 的产品", id);
            return null;
        }

        // 2. 拷贝到 DTO 对象
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        // 3. 查询并设置分类名称
        Long categoryId = dish.getCategoryId();
        if (categoryId != null) {
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                dishDto.setCategoryName(category.getName());
                log.debug("找到分类名称: {}", category.getName());
            } else {
                log.warn("未找到产品 ID {} 对应的分类 ID {}", id, categoryId);
                // 可以根据业务决定是否设置默认名称或保持 null
            }
        } else {
            log.warn("产品 ID {} 没有设置分类 ID", id);
        }

        // 4. 不再需要查询口味信息

        return dishDto;
    }

    /**
     * 更新产品信息（不含口味）
     * @param dishDto 包含需要更新的产品信息（必须有ID）
     */
    @Override
    @Transactional // 开启事务
    public void updateDish(DishDto dishDto) {
        log.info("开始更新产品（无口味）: ID={}, 名称={}", dishDto.getId(), dishDto.getName());

        // 1. 更新 dish 表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDto, dish); // 确保 dishDto 中包含 id

        if (dish.getId() == null) {
            log.error("更新产品失败，未提供产品ID");
            throw new CustomException("更新产品失败，缺少产品ID");
        }

        // 使用 ServiceImpl 提供的 updateById 方法
        boolean updateResult = this.updateById(dish);

        if (!updateResult) {
            // 可能是 ID 不存在或其他原因
            log.error("更新产品基本信息失败: ID={}", dish.getId());
            throw new CustomException("更新产品信息失败，请稍后重试");
        }
        log.info("产品基本信息更新成功: ID={}", dish.getId());

        // 2. 不再需要处理口味的更新
    }

    /**
     * 批量更新产品售卖状态
     * @param status 目标状态 (0:停售, 1:启售)
     * @param ids    需要更新的产品ID列表
     */
    @Override
    @Transactional // 建议对批量更新操作加事务
    public void updateStatus(Integer status, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("批量更新状态操作被调用，但 ID 列表为空");
            return; // 或者根据业务抛出参数异常
        }
        if (status == null || (status != 0 && status != 1)) {
            log.error("无效的状态值: {}", status);
            throw new CustomException("无效的售卖状态值");
        }
        log.info("批量更新产品状态: Status={}, IDs={}", status, ids);

        // 使用 LambdaUpdateWrapper 进行批量更新
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId, ids); // 条件：ID 在列表中
        updateWrapper.set(Dish::getStatus, status); // 设置：更新 status 字段

        boolean updateResult = this.update(updateWrapper); // 执行批量更新

        // MP 的 update(wrapper) 返回值可能不精确反映更新条数，但返回 false 通常意味着有问题
        if (!updateResult) {
            log.warn("批量更新产品状态操作返回 false, 可能未完全成功, Status={}, IDs={}", status, ids);
            // 不一定需要抛异常，取决于业务严格程度
        } else {
            log.info("批量更新产品状态成功执行"); // 注意，这不保证所有 ID 都实际被更新了（例如ID不存在）
        }
    }

    /**
     * 根据 ID 列表删除产品（逻辑或物理删除，取决于 Dish 实体是否配置了 @TableLogic）
     * @param ids 需要删除的产品 ID 列表
     */
    @Override
    @Transactional // 建议对删除操作加事务
    public void deleteDishByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("删除产品操作被调用，但 ID 列表为空");
            return;
        }
        log.info("准备删除产品，IDs: {}", ids);

        // 1. （可选）添加业务检查：如产品是否启售中，是否关联了未删除的套餐等
        // LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // queryWrapper.in(Dish::getId, ids).eq(Dish::getStatus, 1);
        // long activeCount = this.count(queryWrapper);
        // if(activeCount > 0){
        //     throw new CustomException("选中的产品中有正在售卖的，不能删除");
        // }
        // ...检查套餐关联...

        // 2. 删除产品表中的数据
        // removeByIds 会根据 Dish 实体上的 @TableLogic 注解决定行为
        boolean removeResult = this.removeByIds(ids);

        if (!removeResult) {
            // 同 update, removeByIds 可能不会精确反映是否所有都成功删除了。
            log.warn("删除产品操作返回 false, 可能未完全成功, IDs={}", ids);
            // 可以考虑查询数据库确认，但通常日志记录足够
        } else {
            log.info("删除产品（逻辑或物理）成功执行, IDs={}", ids);
        }

        // 3. 不再需要删除关联的口味数据
    }
}