package com.sky.mapper;

import com.sky.entity.Setmeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper//套餐mapper
public interface SetmealMapper {
    @Select("select count(id) from setmeal where category_id=#{id}")
    Integer countByCategoryId(Long id);

    /**
     * 根据菜品id查询套餐
     * @param dishId
     * @return
     */
    @Select("select * from setmeal where dish_id=#{dishId}")
    List<Setmeal> getByDishId(Long dishId);
}
