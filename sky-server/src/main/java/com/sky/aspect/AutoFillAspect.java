package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面,实现公共字段自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点:com.sky.mapper 包下所有方法,且方法上标注了 @AutoFill 注解
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPoinCut() {
    }

    /**
     * 前置通知,在 mapper 方法执行前给实体对象填充公共字段
     *
     * @param joinPoint
     */
    @Before("autoFillPoinCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");

        //1.获取当前被拦截的方法上的 AutoFill 注解,判断操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        //2.获取方法参数(约定第一个参数是实体对象)
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];

        //3.准备要填充的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //4.根据操作类型填充对应字段
        if (operationType == OperationType.INSERT) {
            setFieldValue(entity, AutoFillConstant.SET_CREATE_TIME, now);
            setFieldValue(entity, AutoFillConstant.SET_UPDATE_TIME, now);
            setFieldValue(entity, AutoFillConstant.SET_CREATE_USER, currentId);
            setFieldValue(entity, AutoFillConstant.SET_UPDATE_USER, currentId);
        } else if (operationType == OperationType.UPDATE) {
            setFieldValue(entity, AutoFillConstant.SET_UPDATE_TIME, now);
            setFieldValue(entity, AutoFillConstant.SET_UPDATE_USER, currentId);
        }
    }

    /**
     * 反射调用实体对象的 setter 方法进行赋值
     *
     * @param entity     实体对象
     * @param methodName setter 方法名
     * @param value      要赋的值
     */
    private void setFieldValue(Object entity, String methodName, Object value) {
        try {
            Method method = entity.getClass().getMethod(methodName, value.getClass());
            method.invoke(entity, value);
        } catch (Exception e) {
            log.error("公共字段自动填充失败:{}", e.getMessage());
        }
    }
}
