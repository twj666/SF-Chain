package com.sfchain.spring.boot.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: SFChain配置属性类
 * @author suifeng
 * 日期: 2025/4/15
 */
@Data
@ConfigurationProperties(prefix = "sfchain")
public class SFChainProperties {
    
    /**
     * 模型配置
     */
    private Map<String, ModelProperties> models = new HashMap<>();
    
    /**
     * 模型属性类
     */
    @Data
    public static class ModelProperties {
        /**
         * API基础URL
         */
        private String baseUrl;
        
        /**
         * API密钥
         */
        private String apiKey;
        
        /**
         * 默认模型版本
         */
        private String defaultModel;
        
        /**
         * 温度参数
         */
        private Double temperature = 0.7;
        
        /**
         * 最大token数
         */
        private Integer maxTokens = 2048;
        
        /**
         * 系统提示词
         */
        private String systemPrompt;
    }
}