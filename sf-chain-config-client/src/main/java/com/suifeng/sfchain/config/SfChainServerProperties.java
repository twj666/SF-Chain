package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.management.ManagementFactory;
import java.util.UUID;

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
     * 当前租户应用实例ID（用于配置中心在线状态识别）
     */
    private String instanceId = buildDefaultInstanceId();

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeoutMs = 3000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeoutMs = 5000;

    /**
     * 是否启用治理回调签名
     */
    private boolean callbackSignatureEnabled = false;

    /**
     * 治理回调签名密钥
     */
    private String callbackSigningSecret;

    /**
     * 是否启用配置中心响应签名校验
     */
    private boolean responseSignatureEnabled = false;

    /**
     * 配置中心响应签名密钥
     */
    private String responseSigningSecret;

    /**
     * 响应签名时间戳允许偏差（秒）
     */
    private int responseSignatureMaxSkewSeconds = 300;

    /**
     * 响应签名重放保护窗口（秒）
     */
    private int responseSignatureReplayWindowSeconds = 600;

    private static String buildDefaultInstanceId() {
        try {
            String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
            if (runtimeName != null && !runtimeName.isBlank()) {
                return runtimeName;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return UUID.randomUUID().toString();
    }
}
