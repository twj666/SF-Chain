package com.suifeng.sfchain.configcenter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.configcenter.logging.AICallLogRouteContext;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.FileAICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "sf-chain.ingestion", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({SfChainIngestionProperties.class, SfChainLoggingProperties.class})
public class IngestionApiConfiguration {

    private static final String CONFIG_CENTER_TENANT_ID = "__config_center__";
    private static final String CONFIG_CENTER_APP_ID = "__config_center__";

    @Bean
    @Primary
    public AICallLogManager aiCallLogManager(
            SfChainLoggingProperties loggingProperties,
            ObjectProvider<AICallLogUploadGateway> uploadGatewayProvider,
            ObjectProvider<AICallLogIngestionStore> ingestionStoreProvider) {
        AICallLogUploadGateway uploadGateway = uploadGatewayProvider.getIfAvailable(() -> {
            AICallLogIngestionStore ingestionStore = ingestionStoreProvider.getIfAvailable(() -> AICallLogIngestionStore.NO_OP);
            return callLog -> {
                AICallLogRouteContext.RouteKey routeKey = AICallLogRouteContext.current();
                String tenantId = routeKey == null ? CONFIG_CENTER_TENANT_ID : routeKey.getTenantId();
                String appId = routeKey == null ? CONFIG_CENTER_APP_ID : routeKey.getAppId();
                ingestionStore.saveBatch(
                        tenantId,
                        appId,
                        List.of(AICallLogUploadItem.from(callLog, loggingProperties.isUploadContent()))
                );
            };
        });
        return new AICallLogManager(loggingProperties, uploadGateway);
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
        log.info("接入日志文件存储已启用: dir={}", properties.getFilePersistenceDir());
        return new FileAICallLogIngestionStore(objectMapper, properties);
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
