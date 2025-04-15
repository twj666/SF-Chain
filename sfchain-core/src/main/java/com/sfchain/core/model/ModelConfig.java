package com.sfchain.core.model;

import lombok.Data;

/**
 * 描述: 模型配置抽象类，包含模型连接所需的基本配置
 * @author suifeng
 * 日期: 2025/4/15
 */
@Data
public abstract class ModelConfig {
    
    /**
     * API基础URL
     */
    private String baseUrl;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 模型版本
     */
    private String version;
}