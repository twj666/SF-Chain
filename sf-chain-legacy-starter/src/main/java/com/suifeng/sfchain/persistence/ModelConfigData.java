package com.suifeng.sfchain.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: 模型配置数据类
 * 用于持久化存储的模型配置信息
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelConfigData {
    
    /**
     * 模型名称
     */
    private String modelName;
    
    /**
     * API基础URL
     */
    private String baseUrl;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 默认最大token数
     */
    @Builder.Default
    private Integer defaultMaxTokens = 4096;
    
    /**
     * 默认温度参数
     */
    @Builder.Default
    private Double defaultTemperature = 0.7;
    
    /**
     * 是否支持流式输出
     */
    @Builder.Default
    private Boolean supportStream = false;
    
    /**
     * 是否支持JSON格式输出
     */
    @Builder.Default
    private Boolean supportJsonOutput = false;
    
    /**
     * 是否支持思考模式
     */
    @Builder.Default
    private Boolean supportThinking = false;
    
    /**
     * 额外的HTTP请求头
     */
    @Builder.Default
    private Map<String, String> additionalHeaders = new HashMap<>();
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * 模型提供商
     */
    private String provider;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 更新时间戳
     */
    private Long updatedAt;
    
    /**
     * 验证配置是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return modelName != null && !modelName.trim().isEmpty() &&
               baseUrl != null && !baseUrl.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
        if (this.createdAt == null) {
            this.createdAt = this.updatedAt;
        }
    }
}