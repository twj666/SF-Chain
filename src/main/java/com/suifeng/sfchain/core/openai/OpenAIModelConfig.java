package com.suifeng.sfchain.core.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: OpenAI兼容的模型配置
 * @author suifeng
 * 日期: 2025/8/11
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIModelConfig {
    
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
    private Integer defaultMaxTokens;
    
    /**
     * 默认温度参数
     */
    private Double defaultTemperature;
    
    /**
     * 是否支持流式输出
     */
    private Boolean supportStream;
    
    /**
     * 是否支持JSON格式输出
     */
    private Boolean supportJsonOutput;
    
    /**
     * 是否支持思考模式
     */
    private Boolean supportThinking;
    
    /**
     * 额外的HTTP请求头
     */
    private Map<String, String> additionalHeaders;
    
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
    private Boolean enabled;
    
    /**
     * 获取额外请求头，如果为null则返回空Map
     */
    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders != null ? additionalHeaders : new HashMap<>();
    }
    
    /**
     * 添加额外请求头
     */
    public void addHeader(String key, String value) {
        if (additionalHeaders == null) {
            additionalHeaders = new HashMap<>();
        }
        additionalHeaders.put(key, value);
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isValid() {
        return modelName != null && !modelName.trim().isEmpty() &&
               baseUrl != null && !baseUrl.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * 获取默认配置的构建器
     */
    public static OpenAIModelConfigBuilder defaultConfig() {
        return OpenAIModelConfig.builder()
            .defaultMaxTokens(4096)
            .defaultTemperature(0.7)
            .supportStream(false)
            .supportJsonOutput(false)
            .supportThinking(false)
            .enabled(true);
    }
}