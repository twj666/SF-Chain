package com.suifeng.sfchain.lite;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Set;

/**
 * Filters out heavy legacy auto-configurations for lite starter consumers.
 */
public class LiteAutoConfigurationImportFilter implements AutoConfigurationImportFilter {

    private static final Set<String> BLOCKED = Set.of(
            "com.suifeng.sfchain.config.SfChainPersistenceAutoConfiguration",
            "com.suifeng.sfchain.config.SfChainManagementAutoConfiguration",
            "com.suifeng.sfchain.config.SfChainPersistenceManagementAutoConfiguration",
            "com.suifeng.sfchain.config.SfChainStaticUiAutoConfiguration",
            "com.suifeng.sfchain.config.SfChainLogIngestionAutoConfiguration"
    );

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matched = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matched[i] = !BLOCKED.contains(autoConfigurationClasses[i]);
        }
        return matched;
    }
}
