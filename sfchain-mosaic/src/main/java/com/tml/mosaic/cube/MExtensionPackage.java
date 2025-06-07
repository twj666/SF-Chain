package com.tml.mosaic.cube;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MExtensionPackage {

    /**
     * 扩展包ID
     */
    String value();
    
    /**
     * 扩展包名称
     */
    String name() default "";
    
    /**
     * 扩展包描述
     */
    String description() default "";
    
    /**
     * 扩展包版本
     */
    String version() default "1.0.0";
    
    /**
     * 关联的Cube ID
     */
    String cubeId();
}