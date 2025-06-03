package com.tml.mosaic.core.frame;

/**
 * 描述: 扩展点接口
 * @author suifeng
 * 日期: 2025/5/27 
 */
public interface ExtensionPoint {

    /**
     * 获取扩展点ID
     */
    String getExtensionId();

    /**
     * 获取扩展点名称
     */
    String getExtensionName();

    /**
     * 获取扩展点描述
     */
    String getDescription();

    /**
     * 执行扩展点逻辑
     * @param input 输入参数
     * @return 执行结果
     */
    PointResult execute(PointParam input);
}