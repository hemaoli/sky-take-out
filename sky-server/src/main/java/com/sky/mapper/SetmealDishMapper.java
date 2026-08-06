package com.sky.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    // SELECT setmeal_id FROM setmeal_dish WHERE dish_id IN (?,?,?)
    List<Long> getSetmealIdsByDishIds(@Param("dishIds") List<Long> dishIds);

}

