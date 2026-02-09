package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SF-Chain 日志配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "sf-chain.logging")
public class SfChainLoggingProperties {
    
    /**
     * AI调用日志缓存最大容量
     */
    private int aiCallMaxCapacity = 100;
}
