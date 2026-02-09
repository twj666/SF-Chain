package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainConfigSyncProperties;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteConfigSyncServiceTest {

    @Test
    void shouldApplyRemoteSnapshotToModelFactoryAndOperationRegistry() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        RemoteConfigClient client = new RemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();

        OpenAIModelFactory modelFactory = new OpenAIModelFactory();
        AIOperationRegistry operationRegistry = new AIOperationRegistry();

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                modelFactory,
                operationRegistry,
                objectMapper,
                null
        );

        OpenAIModelConfig model = OpenAIModelConfig.builder()
                .modelName("remote-model")
                .baseUrl("https://api.example.com")
                .apiKey("sk-test")
                .provider("test")
                .enabled(true)
                .build();

        AIOperationRegistry.OperationConfig operationConfig = new AIOperationRegistry.OperationConfig();
        operationConfig.setEnabled(true);
        operationConfig.setMaxTokens(256);
        operationConfig.setTemperature(0.2);
        operationConfig.setRequireJsonOutput(false);
        operationConfig.setSupportThinking(false);

        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion("v1");
        snapshot.setModels(Map.of("remote-model", model));
        snapshot.setOperationModelMapping(Map.of("op-a", "remote-model"));
        snapshot.setOperationConfigs(Map.of("op-a", operationConfig));

        syncService.applySnapshot(snapshot);

        assertThat(modelFactory.getModelConfig("remote-model")).isNotNull();
        assertThat(operationRegistry.getModelMapping()).containsEntry("op-a", "remote-model");
        assertThat(operationRegistry.getConfigs().get("op-a").getMaxTokens()).isEqualTo(256);
    }

    @Test
    void shouldApplyGovernanceAndPushFeedbackWhenSyncingSnapshot() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceFeedbackEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(true);

        SfChainIngestionProperties ingestionProperties = new SfChainIngestionProperties();
        ingestionProperties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(ingestionProperties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                ingestionProperties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );

        OpenAIModelFactory modelFactory = new OpenAIModelFactory();
        AIOperationRegistry operationRegistry = new AIOperationRegistry();
        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                modelFactory,
                operationRegistry,
                objectMapper,
                applier
        );

        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion("v2");
        RemoteIngestionGovernanceSnapshot governance = new RemoteIngestionGovernanceSnapshot();
        governance.setContractAllowlist(List.of("v2", "v1"));
        snapshot.setIngestionGovernance(governance);
        client.snapshot = snapshot;

        syncService.syncOnce(true);

        assertThat(ingestionProperties.getSupportedContractVersion()).isEqualTo("v2");
        assertThat(ingestionProperties.getSupportedContractVersions()).containsExactly("v2", "v1");
        assertThat(client.lastFeedbackVersion).isEqualTo("v2");
        assertThat(client.lastFeedbackResult).isNotNull();
        assertThat(client.lastFeedbackResult.isApplied()).isTrue();
        assertThat(client.lastFeedbackResult.getStatus()).isEqualTo(GovernanceReleaseStatus.SUCCEEDED);
    }

    private static class StubRemoteConfigClient extends RemoteConfigClient {

        private RemoteConfigSnapshot snapshot;
        private String lastFeedbackVersion;
        private GovernanceSyncApplyResult lastFeedbackResult;

        StubRemoteConfigClient(ObjectMapper objectMapper, SfChainServerProperties serverProperties) {
            super(objectMapper, serverProperties);
        }

        @Override
        public Optional<RemoteConfigSnapshot> fetchSnapshot(String currentVersion) {
            return Optional.ofNullable(snapshot);
        }

        @Override
        public void pushGovernanceFeedback(String snapshotVersion, GovernanceSyncApplyResult result) {
            this.lastFeedbackVersion = snapshotVersion;
            this.lastFeedbackResult = result;
        }
    }
}
