package com.suifeng.sfchain.configcenter.config;

import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.configcenter.logging.AICallLogRouteContext;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadGateway;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class AICallLogPersistenceConfiguration {

    private static final String CONFIG_CENTER_TENANT_ID = "__config_center__";
    private static final String CONFIG_CENTER_APP_ID = "__config_center__";

    @Bean
    @ConditionalOnProperty(prefix = "sf-chain.ingestion", name = "enabled", havingValue = "true")
    @ConditionalOnBean(name = "fileAICallLogIngestionStore")
    public AICallLogUploadGateway localIngestionUploadGateway(
            SfChainLoggingProperties loggingProperties,
            AICallLogIngestionStore ingestionStore) {
        log.info("配置中心AI日志持久化已启用: store={}", ingestionStore.getClass().getSimpleName());
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
    }
}
