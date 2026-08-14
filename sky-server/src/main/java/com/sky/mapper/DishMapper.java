package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     * @param dish
     */

    @AutoFill(value = OperationType.INSERT)
    void insert( Dish dish);

    /**
     * 菜品查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    /**
     * 根据id删除菜品数据
     * @param id
     */


    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    /**
     * 修改菜品数据
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据菜品id集合批量查询菜品
     * @param ids
     * @return
     */
    @Select("<script>select * from dish where id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Dish> getByIds(@Param("ids") List<Long> ids);

    /**
     * 根据分类id查询起售中的菜品
     * @param categoryId
     * @return
     */
    @Select("select * from dish where category_id = #{categoryId} and status = 1")
    List<Dish> listByCategoryId(Long categoryId);

    /**
     * 扣减菜品库存:库存足够才扣,防止并发超卖
     * @param id 菜品id
     * @param num 扣减数量
     * @return 受影响行数,0表示库存不足
     */
    @Update("update dish set stock = stock - #{num} where id = #{id} and stock >= #{num}")
    int deductStock(@Param("id") Long id, @Param("num") Integer num);

    /**
     * 动态条件查询菜品(用户端商品浏览使用)
     * @param dish
     * @return
     */
    List<Dish> list(Dish dish);
}
