package com.suifeng.sfchain.configcenter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.FileAICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sf-chain.ingestion", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({SfChainIngestionProperties.class, SfChainLoggingProperties.class})
public class IngestionApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AICallLogManager aiCallLogManager(SfChainLoggingProperties loggingProperties) {
        return new AICallLogManager(loggingProperties);
    }

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
    public AICallLogIngestionStore fileAICallLogIngestionStore(
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
}
