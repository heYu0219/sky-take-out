package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper//套餐mapper
public interface SetmealMapper {
    @Select("select count(id) from setmeal where category_id=#{id}")
    Integer countByCategoryId(Long id);
}
