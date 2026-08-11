package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import java.util.List;

import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */

    @PostMapping
    @ApiOperation(value = "新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}", dishDTO);

        dishService.saveWithFlavor(dishDTO);
        return  Result.success();
    }

    @GetMapping("/page")
    @ApiOperation(value = "菜品分页查询")
    public  Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){

        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return  Result.success(pageResult);
    }

    /**
     * 根据分类id查询菜品列表(添加套餐选择菜品时使用)
     */
    @GetMapping("/list")
    @ApiOperation(value = "根据分类id查询菜品列表")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品：{}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 批量删除菜品
     */
    @DeleteMapping
    @ApiOperation(value = "批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除菜品：{}", ids);
        dishService.deleteBatch(ids);
        return  Result.success();
    }

    /**
     * 根据id查询菜品和对应的口味
     */

    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询菜品")
public Result<DishVO> getById(@PathVariable Long id){
    log.info("根据id查询菜品信息：{}", id);
    DishVO dishVO = dishService.getByIdWithFlavor(id);

    return  Result.success(dishVO);
}

    @PutMapping
    @ApiOperation(value = "修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("更新菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return  Result.success();

    }

    /**
     * 菜品起售停售
     */
    @PostMapping("/status/{status}")
    @ApiOperation(value = "菜品起售停售")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("菜品起售停售：{} {}", status, id);
        dishService.startOrStop(status, id);
        return Result.success();
    }
}
