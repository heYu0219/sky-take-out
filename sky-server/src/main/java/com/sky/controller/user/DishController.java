package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        //根据分类id查询菜品：如果从未查询过，则查询数据库将数据放入缓存 否则直接返回缓存中的数据
        //构造redis的key：dish_分类id
        String key="dish_"+categoryId;

        //构建查询对象
        ValueOperations operations = redisTemplate.opsForValue();
        //查询数据 放入的是什么类型的数据 取出的就是什么类型的数据
        List<DishVO> list= (List<DishVO>) operations.get(key);
        //判断缓存中是否存在该分类下的菜品数据
        if(list!=null && list.size()>0){
            //存在，直接返回
            return  Result.success(list);
        }
        //不存在 查询数据库 并将其放入缓存
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        list = dishService.listWithFlavor(dish);
        //将其放入缓存
        operations.set(key,list);
        return Result.success(list);
    }


}
