package com.tml.mosaic.core.annotation;

import java.lang.annotation.*;

/**
 * 描述: 扩展点注解，标识这是一个扩展点
 * @author suifeng
 * 日期: 2025/5/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MExtension {

    /**
     * 扩展点ID
     */
    String value();

    /**
     * 扩展点名称
     */
    String name() default "";

    /**
     * 扩展点描述
     */
    String description() default "";

    /**
     * 优先级，数字越小优先级越高
     */
    int priority() default 100;
}