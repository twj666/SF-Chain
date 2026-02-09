package com.suifeng.sfchain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainConfigSyncProperties;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.suifeng.sfchain.config.remote.RemoteConfigClient;
import com.suifeng.sfchain.config.remote.RemoteConfigSyncService;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AICallLogGovernanceControllerTest {

    @Test
    void shouldReturnGovernanceSyncMetricsWhenServiceAvailable() {
        SfChainIngestionProperties ingestionProperties = new SfChainIngestionProperties();
        ingestionProperties.setApiKey("center-key");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(ingestionProperties);

        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");
        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                new RemoteConfigClient(new ObjectMapper(), serverProperties),
                new SfChainConfigSyncProperties(),
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                new ObjectMapper(),
                null
        );

        AICallLogGovernanceController controller = new AICallLogGovernanceController(
                ingestionProperties,
                guardService,
                providerFor(IngestionIndexMaintenanceService.class, null),
                providerFor(RemoteConfigSyncService.class, syncService)
        );

        ResponseEntity<?> response = controller.governanceSyncMetrics("center-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("enabled")).isEqualTo(true);
        assertThat(body.get("metrics")).isNotNull();
    }

    @Test
    void shouldReturnDisabledWhenGovernanceSyncServiceAbsent() {
        SfChainIngestionProperties ingestionProperties = new SfChainIngestionProperties();
        ingestionProperties.setApiKey("center-key");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(ingestionProperties);

        AICallLogGovernanceController controller = new AICallLogGovernanceController(
                ingestionProperties,
                guardService,
                providerFor(IngestionIndexMaintenanceService.class, null),
                providerFor(RemoteConfigSyncService.class, null)
        );

        ResponseEntity<?> response = controller.governanceSyncMetrics("center-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("enabled", false));
    }

    private static <T> ObjectProvider<T> providerFor(Class<T> type, T bean) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        if (bean != null) {
            factory.registerSingleton(type.getName(), bean);
        }
        return factory.getBeanProvider(type);
    }
}
