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

    /**
     * 是否要求tenantId与appId必须传入
     */
    private boolean requireTenantApp = true;

    /**
     * 每个租户应用每分钟最大接收条数
     */
    private int perTenantAppPerMinuteLimit = 5000;

    /**
     * 是否启用文件持久化
     */
    private boolean filePersistenceEnabled = true;

    /**
     * 持久化目录（jsonl）
     */
    private String filePersistenceDir = ".sf-chain/ingestion-logs";

    /**
     * 文件保留天数
     */
    private int retentionDays = 7;

    /**
     * 查询最大返回条数
     */
    private int maxQueryLimit = 500;
}
