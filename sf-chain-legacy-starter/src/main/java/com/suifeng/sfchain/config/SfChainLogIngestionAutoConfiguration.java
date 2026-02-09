package com.suifeng.sfchain.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.remote.IngestionGovernanceSyncApplier;
import com.suifeng.sfchain.controller.AICallLogGovernanceController;
import com.suifeng.sfchain.controller.AICallLogIngestionController;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.FileAICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SF-Chain 配置中心日志接入API自动配置
 */
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.features.management-api:false} and ${sf-chain.ingestion.enabled:false}")
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(SfChainIngestionProperties.class)
@Import({AICallLogIngestionController.class, AICallLogGovernanceController.class})
public class SfChainLogIngestionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinuteWindowQuotaService minuteWindowQuotaService(SfChainIngestionProperties properties) {
        return new MinuteWindowQuotaService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IngestionContractHealthTracker ingestionContractHealthTracker() {
        return new IngestionContractHealthTracker();
    }

    @Bean
    @ConditionalOnProperty(prefix = "sf-chain.ingestion", name = "file-persistence-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public AICallLogIngestionStore aiCallLogIngestionStore(
            ObjectMapper objectMapper,
            SfChainIngestionProperties properties) {
        return new FileAICallLogIngestionStore(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AICallLogIngestionStore noOpAICallLogIngestionStore() {
        return AICallLogIngestionStore.NO_OP;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "sf-chain.ingestion",
            name = {"index-enabled", "index-maintenance-enabled"},
            havingValue = "true")
    @ConditionalOnMissingBean
    public IngestionIndexMaintenanceService ingestionIndexMaintenanceService(
            SfChainIngestionProperties properties,
            AICallLogIngestionStore ingestionStore) {
        return new IngestionIndexMaintenanceService(properties, ingestionStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public ContractAllowlistGuardService contractAllowlistGuardService(SfChainIngestionProperties properties) {
        return new ContractAllowlistGuardService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IngestionGovernanceSyncApplier ingestionGovernanceSyncApplier(
            SfChainIngestionProperties properties,
            ContractAllowlistGuardService guardService,
            ObjectProvider<IngestionIndexMaintenanceService> indexMaintenanceServiceProvider,
            ObjectProvider<IngestionContractHealthTracker> healthTrackerProvider,
            ObjectProvider<SfChainServerProperties> serverPropertiesProvider) {
        SfChainServerProperties serverProperties = serverPropertiesProvider.getIfAvailable();
        String appId = serverProperties == null ? "default" : serverProperties.getAppId();
        return new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                indexMaintenanceServiceProvider.getIfAvailable(),
                healthTrackerProvider.getIfAvailable(),
                appId
        );
    }
}
