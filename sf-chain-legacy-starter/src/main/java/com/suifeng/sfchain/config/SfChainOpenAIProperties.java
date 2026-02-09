package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SF-Chain OpenAI HTTP配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "sf-chain.openai")
public class SfChainOpenAIProperties {
    
    /**
     * 连接超时（毫秒）
     */
    private int connectTimeoutMs = 30000;
    
    /**
     * 读取超时（毫秒）
     */
    private int readTimeoutMs = 300000;
}
