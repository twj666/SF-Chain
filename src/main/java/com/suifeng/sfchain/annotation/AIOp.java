package com.suifeng.sfchain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述: AI操作注解
 * 用于标识AI操作类，并指定操作类型和默认模型
 * @author suifeng
 * 日期: 2025/8/11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIOp {
    
    /**
     * 操作类型标识
     * 例如: "POSITION_BASIC_INFO_PARSE_OP"
     */
    String value();
    
    /**
     * 默认使用的模型名称
     * 如果不指定，将使用操作映射配置中的模型
     */
    String defaultModel() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
    
    /**
     * 是否启用该操作
     */
    boolean enabled() default true;
    
    /**
     * 支持的模型列表（可选）
     * 如果指定，将限制该操作只能使用这些模型
     */
    String[] supportedModels() default {};
    
    /**
     * 是否需要JSON输出
     */
    boolean requireJsonOutput() default true;
    
    /**
     * 是否自动修复JSON格式错误
     * 当requireJsonOutput为true且AI返回的JSON格式有误时，自动调用JSON修复操作
     */
    boolean autoRepairJson() default true;
    
    /**
     * 是否支持思考模式
     */
    boolean supportThinking() default false;
    
    /**
     * 默认最大token数
     */
    int defaultMaxTokens() default 4096;
    
    /**
     * 默认温度参数
     */
    double defaultTemperature() default 0.7;
}