package com.sky.service;

import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.enumeration.OperationType;

public interface DishService {
    //新增菜品

    void saveWithFlavor(DishDTO dishDTO);
}
