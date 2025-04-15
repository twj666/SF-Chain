package com.sfchain.core.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 描述: 模型参数类，用于配置模型生成行为
 * @author suifeng
 * 日期: 2025/4/15
 */
@Data
@Accessors(fluent = true)
public class ModelParameters {
    
    /**
     * 温度参数，控制随机性 (0.0-2.0)
     */
    private Double temperature = 0.7;
    
    /**
     * 最大生成token数
     */
    private Integer maxTokens = 2048;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 创建默认参数实例
     */
    public ModelParameters() {
    }
    
    /**
     * 从现有参数复制创建新实例
     * 
     * @param other 其他参数实例
     */
    public ModelParameters(ModelParameters other) {
        if (other != null) {
            this.temperature = other.temperature;
            this.maxTokens = other.maxTokens;
            this.systemPrompt = other.systemPrompt;
        }
    }
}