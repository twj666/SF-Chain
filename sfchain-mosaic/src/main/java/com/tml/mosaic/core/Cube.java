package com.tml.mosaic.core;

/**
 * 描述: 插件的基础接口
 * @author suifeng
 * 日期: 2025/5/27
 */
public interface Cube {

    /**
     * 获取Cube的唯一标识
     */
    String getCubeId();
    
    /**
     * 获取Cube的版本
     */
    String getVersion();
    
    /**
     * 获取Cube的描述信息
     */
    String getDescription();
    
    /**
     * Cube初始化方法
     */
    void initialize();
    
    /**
     * Cube销毁方法
     */
    void destroy();
}