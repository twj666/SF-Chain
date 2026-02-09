package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainConfigSyncProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceSyncMetricsBinderTest {

    @Test
    void shouldBindGovernanceSyncMetricsToMeterRegistry() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                new RemoteConfigClient(objectMapper, serverProperties),
                new SfChainConfigSyncProperties(),
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        GovernanceSyncMetricsBinder binder = new GovernanceSyncMetricsBinder(syncService);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        binder.bindTo(registry);

        assertThat(registry.find("sfchain.governance.sync.run.count").gauge()).isNotNull();
        assertThat(registry.find("sfchain.governance.finalize.reconcile.invalid_cursor.count").gauge()).isNotNull();
        assertThat(registry.find("sfchain.governance.finalize.reconcile.cursor_reset.count").gauge()).isNotNull();
        assertThat(registry.find("sfchain.governance.finalize.reconcile.invalid_cursor.fail_fast.count").gauge()).isNotNull();
    }
}
