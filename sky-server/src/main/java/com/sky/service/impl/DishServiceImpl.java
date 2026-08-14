package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.utils.RedisTtlUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j

public class DishServiceImpl implements DishService {
    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDTO
     */

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Transactional

    public void saveWithFlavor(DishDTO dishDTO) {
        //向菜品表插入1条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //前端未传库存时,给默认库存0,避免插入null违反NOT NULL约束
        if (dish.getStock() == null) {
            dish.setStock(0);
        }
        dishMapper.insert(  dish);
        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        //向口味表插入n条数据
List<DishFlavor> flavors = dishDTO.getFlavors();
if ( flavors != null && flavors.size() > 0)
{
    flavors.forEach(dishFlavor ->
            dishFlavor.setDishId(dishId));

    dishFlavorMapper.insertBatch(flavors);
}
        //清理该分类下的菜品缓存
        cleanCache(dishDTO.getCategoryId());

    }

    /**
     * 菜品查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page= dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除
     * @param ids
     */

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
//        判断菜品是否可以删除--菜品是否在售
        List<Long> categoryIds = new ArrayList<>();
        for (Long id : ids){
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            categoryIds.add(dish.getCategoryId());
        }

//        判断菜品是否可以删除--关联了套餐
List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
//        删除菜品表中的数据
        for (Long id : ids)
        {
            dishMapper.deleteById(id);
            //        删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
        //清理这些菜品所属分类的缓存
        categoryIds.forEach(this::cleanCache);
        }

        /**
         * 根据id查询菜品和对应的口味数据
         * @param id
         * @return
         */

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
       Dish dish = dishMapper.getById(  id);

        //根据菜品id查询口味数据
        List <DishFlavor> dishFlavors =dishFlavorMapper.getByDishId(id);
        //数据封装到DishVO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息以及口味
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //先查原菜品,拿到旧分类,精确删旧分类缓存
        Dish oldDish = dishMapper.getById(dishDTO.getId());

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品表基本信息
        dishMapper.update(dish);


        //删除原本菜品关联的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0)
        {
            flavors.forEach(dishFlavor ->
                    dishFlavor.setDishId(dishDTO.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }
        if (oldDish != null) {
            cleanCache(oldDish.getCategoryId());
        }
        //分类可能发生变化,同时清理新分类的缓存
        cleanCache(dishDTO.getCategoryId());


    }

    /**
     * 根据分类id查询菜品列表
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        return dishMapper.listByCategoryId(categoryId);
    }

    /**
     * 条件查询菜品和对应的口味数据(用户端商品浏览使用)
     * @param dish
     * @return
     */
    @Override

    public List<DishVO> listWithFlavor(Dish dish) {
        //1.构造缓存key,查缓存
        String key = "dish:list:" + dish.getCategoryId();
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            //2.缓存命中(包括空列表),直接返回
            return list;
        }

        //3.缓存未命中,查数据库
        List<Dish> dishList = dishMapper.list(dish);
        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        //4.写入缓存:空列表防穿透用短TTL,正常数据用长TTL
        if (dishVOList == null || dishVOList.size() == 0) {
            //空列表防穿透:固定5分钟,短TTL影响小
            redisTemplate.opsForValue().set(key, dishVOList, 5, TimeUnit.MINUTES);
        } else {
            //随机55~65分钟,避免所有缓存同时过期造成雪崩
            redisTemplate.opsForValue().set(key, dishVOList, RedisTtlUtil.getRandomMinute(), TimeUnit.MINUTES);
        }
        return dishVOList;
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //先查原菜品,拿到分类,清理缓存
        Dish dish = dishMapper.getById(id);
        if (dish != null) {
            cleanCache(dish.getCategoryId());
        }

        Dish updateDish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(updateDish);
    }

    /**
     * 按分类精确删除菜品缓存(避免KEYS命令阻塞Redis)
     * @param categoryId
     */
    private void cleanCache(Long categoryId) {
        if (categoryId != null) {
            String key = "dish:list:" + categoryId;
            redisTemplate.delete(key);
            log.info("清理菜品缓存：{}", key);
        }
    }


}
