package com.sky.mapper;

import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper//套餐mapper
public interface SetmealDishMapper {

    @Select("select * from setmeal_dish where dish_id=#{dishId}")
    List<SetmealDish> getByDishId(Long dishId);
}
