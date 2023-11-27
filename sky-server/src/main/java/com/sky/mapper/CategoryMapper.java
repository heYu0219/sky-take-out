package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper {

    void update(Category category);

    Page<Category> page(CategoryPageQueryDTO categoryPageQueryDTO);

    void insert(Category category);

    @Delete("delete from category where id=#{id}")
    void deleteById(Long id);
}
