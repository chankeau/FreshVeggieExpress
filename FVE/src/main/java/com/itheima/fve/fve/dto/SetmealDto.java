package com.itheima.fve.fve.dto;

import com.itheima.fve.fve.entity.Setmeal;
import com.itheima.fve.fve.entity.SetmealDish;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;


    private String categoryName;
}
