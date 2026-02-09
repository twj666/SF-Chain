package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainConfigSyncProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                objectMapper
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
}
