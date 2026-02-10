package com.suifeng.sfchain.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.remote.GovernanceSyncApplier;
import com.suifeng.sfchain.config.remote.RemoteConfigClient;
import com.suifeng.sfchain.config.remote.RemoteConfigSyncService;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * SF-Chain 远程配置同步自动配置
 */
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.config-sync.enabled:false}")
@EnableConfigurationProperties({
        SfChainServerProperties.class,
        SfChainConfigSyncProperties.class
})
public class SfChainRemoteConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RemoteConfigClient remoteConfigClient(
            ObjectMapper objectMapper,
            SfChainServerProperties serverProperties) {
        return new RemoteConfigClient(objectMapper, serverProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RemoteConfigSyncService remoteConfigSyncService(
            RemoteConfigClient remoteConfigClient,
            SfChainConfigSyncProperties syncProperties,
            OpenAIModelFactory modelFactory,
            AIOperationRegistry operationRegistry,
            ObjectMapper objectMapper,
            ObjectProvider<GovernanceSyncApplier> governanceSyncApplierProvider) {
        return new RemoteConfigSyncService(
                remoteConfigClient,
                syncProperties,
                modelFactory,
                operationRegistry,
                objectMapper,
                governanceSyncApplierProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean
    public Object governanceSyncMetricsBinder(RemoteConfigSyncService syncService) {
        return new com.suifeng.sfchain.config.remote.GovernanceSyncMetricsBinder(syncService);
    }
}
