package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;


    @Override
    @Transactional
    @AutoFill(value = OperationType.INSERT)
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);

        Long id=dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null &&flavors.size()>0){
            for (DishFlavor df:flavors) {
                df.setDishId(id);
            }
        }
        dishFlavorMapper.insertBatch(flavors);

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前要删除的菜品是否在起售中
        for (Long id:ids) {
            Dish dish=dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否关联了套餐
        for(Long id:ids){
            List<SetmealDish> setmealDishes=setmealDishMapper.getByDishId(id);
            if(setmealDishes!=null && setmealDishes.size()>0){
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }

        for (Long dishId :ids){
            //批量删除菜品
            dishMapper.deleteById(dishId);
            //删除与菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(dishId);
        }
    }
}
