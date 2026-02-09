package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SF-Chain 特性开关配置
 */
@Data
@ConfigurationProperties(prefix = "sf-chain.features")
public class SfChainFeaturesProperties {

    /**
     * 是否启用管理API
     */
    private boolean managementApi = false;

    /**
     * 是否启用本地持久化
     */
    private boolean localPersistence = false;

    /**
     * 是否启用本地migration
     */
    private boolean localMigration = false;

    /**
     * 是否启用静态UI
     */
    private boolean staticUi = false;
}
