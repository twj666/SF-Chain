package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 接入契约版本
     */
    private String supportedContractVersion = "v1";

    /**
     * 接入契约版本白名单
     */
    private List<String> supportedContractVersions = new ArrayList<>();

    /**
     * 是否启用行偏移索引
     */
    private boolean indexEnabled = true;

    /**
     * 索引步长（每N行一个偏移）
     */
    private int indexStride = 200;

    /**
     * 是否启用后台索引维护
     */
    private boolean indexMaintenanceEnabled = true;

    /**
     * 后台索引维护间隔（秒）
     */
    private int indexMaintenanceIntervalSeconds = 300;

    /**
     * 是否启用自适应索引步长
     */
    private boolean adaptiveIndexStrideEnabled = true;

    /**
     * 契约白名单最大活跃版本数
     */
    private int maxActiveContractVersions = 2;

    /**
     * 是否要求新白名单与当前白名单存在交集
     */
    private boolean requireCurrentVersionOverlap = true;
}
