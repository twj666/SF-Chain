package com.sfchain.core.operation;

import java.util.List;

/**
 * 描述: AI操作接口，定义AI操作的基本行为
 * @author suifeng
 * 日期: 2025/4/15
 */
public interface AIOperation<P, R> {
    
    /**
     * 获取支持的模型列表
     * 
     * @return 支持的模型名称列表
     */
    List<String> supportedModels();
    
    /**
     * 根据参数构建提示词
     * 
     * @param params 操作参数
     * @return 构建的提示词
     */
    String buildPrompt(P params);
    
    /**
     * 解析AI响应
     * 
     * @param aiResponse AI响应文本
     * @return 解析后的结果
     */
    R parseResponse(String aiResponse);
    
    /**
     * 验证参数
     * 
     * @param params 操作参数
     */
    default void validate(P params) {
        // 默认实现为空，子类可以覆盖实现参数验证逻辑
    }
}