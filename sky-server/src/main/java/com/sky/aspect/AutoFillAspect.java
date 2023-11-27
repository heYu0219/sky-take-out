package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //指定对mapper包下的所有方法执行,并且这些方法上添加了autoFill注解
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    //定义切点
    public void autoFillPointcut(){}

    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段填充...");
        //1、获取当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获取方法上的注解对象
        OperationType operationType = autoFill.value();//获得数据库操作类型

        //2、获取到当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if(args==null||args.length==0){
            return;
        }
        Object entity = args[0];

        //3、准备复制的数据
        LocalDateTime now = LocalDateTime.now();
        Long id = BaseContext.getCurrentId();

        //4、根据当前不同的操作类型为对应的属性通过反射来赋值
        if(operationType==OperationType.INSERT){
            try {
                //获取实体的set方法
                Method setCreateTime=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,id);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(operationType==OperationType.UPDATE){
            try {
                //获取实体的set方法
                Method setUpdateTime=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser=entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
