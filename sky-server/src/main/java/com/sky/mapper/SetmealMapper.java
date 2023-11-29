package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
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

    @AutoFill(value = OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);

    @Select("select * from  setmeal where id=#{id}")
    Setmeal getById(Long id);

    void deleteBatch(List<Long> ids);

    SetmealVO getByIdWithDish(Long id);
}
