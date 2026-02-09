package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SF-Chain 配置同步参数
 */
@Data
@ConfigurationProperties(prefix = "sf-chain.config-sync")
public class SfChainConfigSyncProperties {

    /**
     * 是否启用远程配置同步
     */
    private boolean enabled = false;

    /**
     * 同步间隔（秒）
     */
    private int intervalSeconds = 30;

    /**
     * 远程失败时是否容错继续运行
     */
    private boolean failOpen = true;

    /**
     * 本地快照缓存文件
     */
    private String cacheFile = ".sf-chain/config-snapshot.json";

    /**
     * 是否启用日志治理配置同步
     */
    private boolean ingestionGovernanceEnabled = true;

    /**
     * 是否上报日志治理配置应用结果
     */
    private boolean governanceFeedbackEnabled = true;
}
