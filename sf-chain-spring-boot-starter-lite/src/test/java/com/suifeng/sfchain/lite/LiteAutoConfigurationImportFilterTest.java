package com.suifeng.sfchain.lite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiteAutoConfigurationImportFilterTest {

    @Test
    void shouldBlockHeavyLegacyAutoConfigurations() {
        LiteAutoConfigurationImportFilter filter = new LiteAutoConfigurationImportFilter();
        String[] candidates = {
                "com.suifeng.sfchain.config.SfChainAutoConfiguration",
                "com.suifeng.sfchain.config.SfChainPersistenceAutoConfiguration",
                "com.suifeng.sfchain.config.SfChainManagementAutoConfiguration",
                "com.suifeng.sfchain.config.SfChainRemoteConfigAutoConfiguration",
                "com.suifeng.sfchain.config.SfChainLogUploadAutoConfiguration"
        };

        boolean[] matched = filter.match(candidates, null);

        assertThat(matched).containsExactly(true, false, false, true, true);
    }
}
