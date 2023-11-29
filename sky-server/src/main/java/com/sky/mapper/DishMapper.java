package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper //菜品mapper
public interface DishMapper {
    @Select("select count(id) from dish where category_id=#{id}")
    Integer countByCategoryId(Long id);

    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    @Select("select * from dish where id=#{id}")
    Dish getById(Long id);


    @Delete("delete from dish where id=#{id}")
    void deleteById(Long id);

    void deleteBatch(List<Long> ids);

    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    List<Dish> list(Dish dish);

    @Select("select count(id) from dish where category_id=#{categoryId}")
    Integer getCountByCategoryId(Long categoryId);

    @Select("SELECT d.* from setmeal_dish sd left outer JOIN dish d on sd.dish_id=d.id")
    List<Dish> getBySetmealId(Long setmealId);
}
