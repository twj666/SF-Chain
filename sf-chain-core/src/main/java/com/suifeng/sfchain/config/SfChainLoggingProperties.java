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

    /**
     * 是否启用异步日志上报
     */
    private boolean uploadEnabled = false;

    /**
     * 日志上报接口路径
     */
    private String uploadEndpoint = "/v1/logs/ai-calls/batch";

    /**
     * 上报周期（秒）
     */
    private int uploadIntervalSeconds = 3;

    /**
     * 异步队列容量
     */
    private int queueCapacity = 10000;

    /**
     * 批量上报大小
     */
    private int batchSize = 200;

    /**
     * 失败重试次数
     */
    private int maxRetry = 1;

    /**
     * 日志采样率 [0,1]
     */
    private double sampleRate = 1.0;

    /**
     * 是否上报输入输出内容
     */
    private boolean uploadContent = true;
}
