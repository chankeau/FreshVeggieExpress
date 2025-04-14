package com.itheima.fve.fve.dto;

import com.itheima.fve.fve.entity.Dish;
// import com.itheima.fve.fve.entity.DishFlavor; // 移除口味相关导入
import lombok.Data;
import lombok.EqualsAndHashCode; // 推荐添加，避免某些场景下的问题

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true) // 继承实体类时推荐添加
public class DishDto extends Dish {

    // 移除了 private List<DishFlavor> flavors = new ArrayList<>();

    //商品分类名称
    private String categoryName;

    private String unit;
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    //份数 (这个字段用途不明，看是否实际需要，如果客户端点餐时需要，保留)
    private Integer copies;

    // 默认构造函数（Lombok @Data 会生成）
    // 其他需要的构造函数或方法可以添加
}