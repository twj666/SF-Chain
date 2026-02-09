package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置中心日志接入配置
 */
@Data
@ConfigurationProperties(prefix = "sf-chain.ingestion")
public class SfChainIngestionProperties {

    /**
     * 是否启用日志接入API
     */
    private boolean enabled = false;

    /**
     * 接入API密钥（请求头: X-SF-API-KEY）
     */
    private String apiKey;

    /**
     * 单次最大接收条数
     */
    private int maxBatchSize = 500;
}
