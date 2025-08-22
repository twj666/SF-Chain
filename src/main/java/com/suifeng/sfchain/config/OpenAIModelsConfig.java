package com.suifeng.sfchain.config;

import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: OpenAI兼容模型配置
 * @author suifeng
 * 日期: 2025/8/11
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.openai-models")
public class OpenAIModelsConfig {
    
    /**
     * 模型配置映射
     * key: 模型名称
     * value: 模型配置
     */
    private Map<String, ModelConfigProperties> models = new HashMap<>();
    
    /**
     * 模型配置属性
     */
    @Data
    public static class ModelConfigProperties {
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
        private Integer defaultMaxTokens = 4096;
        
        /**
         * 默认温度参数
         */
        private Double defaultTemperature = 0.7;
        
        /**
         * 是否支持流式输出
         */
        private Boolean supportStream = false;
        
        /**
         * 是否支持JSON格式输出
         */
        private Boolean supportJsonOutput = false;
        
        /**
         * 是否支持思考模式
         */
        private Boolean supportThinking = false;
        
        /**
         * 额外的HTTP请求头
         */
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
        private Boolean enabled = true;
        
        /**
         * 转换为OpenAIModelConfig
         */
        public OpenAIModelConfig toOpenAIModelConfig() {
            return OpenAIModelConfig.builder()
                .modelName(modelName)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultMaxTokens(defaultMaxTokens)
                .defaultTemperature(defaultTemperature)
                .supportStream(supportStream)
                .supportJsonOutput(supportJsonOutput)
                .supportThinking(supportThinking)
                .additionalHeaders(additionalHeaders)
                .description(description)
                .provider(provider)
                .enabled(enabled)
                .build();
        }
    }
    
    /**
     * 获取所有有效的模型配置
     */
    public Map<String, OpenAIModelConfig> getValidModelConfigs() {
        Map<String, OpenAIModelConfig> validConfigs = new HashMap<>();
        
        models.forEach((key, properties) -> {
            if (properties.getModelName() == null) {
                properties.setModelName(key);
            }
            
            OpenAIModelConfig config = properties.toOpenAIModelConfig();
            if (config.isValid() && Boolean.TRUE.equals(config.getEnabled())) {
                validConfigs.put(key, config);
            }
        });
        
        return validConfigs;
    }
}