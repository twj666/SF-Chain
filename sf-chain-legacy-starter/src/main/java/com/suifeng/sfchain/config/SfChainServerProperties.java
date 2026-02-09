package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SF-Chain 远程服务配置
 */
@Data
@ConfigurationProperties(prefix = "sf-chain.server")
public class SfChainServerProperties {

    /**
     * 远程配置中心地址
     */
    private String baseUrl;

    /**
     * 调用配置中心的API Key
     */
    private String apiKey;

    /**
     * 租户ID
     */
    private String tenantId = "default";

    /**
     * 应用ID
     */
    private String appId = "default";

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeoutMs = 3000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeoutMs = 5000;
}
