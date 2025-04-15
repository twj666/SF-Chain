package com.sfchain.models.qwen;

import com.sfchain.core.model.ModelConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 描述: 千问模型配置信息
 * @author suifeng
 * 日期: 2025/4/15
 */
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "sfchain.models.qwen")
@Component
@Data
public class QwenConfig extends ModelConfig {
    
    /**
     * 模型版本
     */
    private String version = "qwen-plus";
    
    /**
     * 温度参数
     */
    private Double temperature = 0.7;
    
    /**
     * 最大token数
     */
    private Integer maxTokens = 4096;
    
    /**
     * 系统提示词
     */
    private String systemPrompt = "You are an expert in java back-end programming";
}