package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SF-Chain 顶层配置
 */
@Data
@ConfigurationProperties(prefix = "sf-chain")
public class SfChainProperties {

    /**
     * SF-Chain 总开关
     */
    private boolean enabled = true;
}

