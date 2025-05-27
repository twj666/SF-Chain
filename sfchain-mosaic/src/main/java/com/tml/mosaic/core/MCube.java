package com.tml.mosaic.core;

import java.lang.annotation.*;

/**
 * 描述: Cube注解，标识这是一个方块
 * @author suifeng
 * 日期: 2025/5/27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MCube {

    /**
     * Cube的ID，如果不指定则使用类名
     */
    String value() default "";
    
    /**
     * 版本号
     */
    String version() default "1.0.0";
    
    /**
     * 描述信息
     */
    String description() default "";
    
    /**
     * 是否自动注册
     */
    boolean autoRegister() default true;
}