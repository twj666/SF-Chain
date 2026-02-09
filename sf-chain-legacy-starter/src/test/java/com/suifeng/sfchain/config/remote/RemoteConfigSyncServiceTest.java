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
import java.nio.file.Files;
import java.nio.file.Path;

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

        syncService.applySnapshot(snapshot, true);

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
        syncProperties.setGovernanceEventEnabled(true);
        syncProperties.setGovernanceFinalizeEnabled(true);

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
        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setReleaseId("release-feedback");
        rollout.setStage("FULL");
        governance.setRollout(rollout);
        snapshot.setIngestionGovernance(governance);
        client.snapshot = snapshot;

        syncService.syncOnce(true);

        assertThat(ingestionProperties.getSupportedContractVersion()).isEqualTo("v2");
        assertThat(ingestionProperties.getSupportedContractVersions()).containsExactly("v2", "v1");
        assertThat(client.lastFeedbackVersion).isEqualTo("v2");
        assertThat(client.lastFeedbackResult).isNotNull();
        assertThat(client.lastFeedbackResult.isApplied()).isTrue();
        assertThat(client.lastFeedbackResult.getStatus()).isEqualTo(GovernanceReleaseStatus.SUCCEEDED);
        assertThat(client.eventPushCount).isEqualTo(1);
        assertThat(client.finalizePushCount).isEqualTo(1);
    }

    @Test
    void shouldFinalizeTerminalStateOnlyOnceForSameReleaseStatus() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(true);

        SfChainIngestionProperties ingestionProperties = new SfChainIngestionProperties();
        ingestionProperties.setSupportedContractVersion("v1");
        ingestionProperties.setRequireCurrentVersionOverlap(false);
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(ingestionProperties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                ingestionProperties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                applier
        );
        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setReleaseId("release-1");
        rollout.setStage("FULL");

        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion("v-terminal-1");
        RemoteIngestionGovernanceSnapshot governance = new RemoteIngestionGovernanceSnapshot();
        governance.setContractAllowlist(List.of("v2", "v1"));
        governance.setRollout(rollout);
        snapshot.setIngestionGovernance(governance);
        client.snapshot = snapshot;

        syncService.syncOnce(true);

        snapshot.setVersion("v-terminal-2");
        syncService.syncOnce(false);

        assertThat(client.finalizePushCount).isEqualTo(1);
    }

    @Test
    void shouldRetryFinalizeWhenAckNotReceived() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        client.finalizeAck = false;
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(true);

        SfChainIngestionProperties ingestionProperties = new SfChainIngestionProperties();
        ingestionProperties.setSupportedContractVersion("v1");
        ingestionProperties.setRequireCurrentVersionOverlap(false);
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(ingestionProperties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                ingestionProperties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );
        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                applier
        );

        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setReleaseId("release-retry");
        rollout.setStage("FULL");
        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion("v-retry-1");
        RemoteIngestionGovernanceSnapshot governance = new RemoteIngestionGovernanceSnapshot();
        governance.setContractAllowlist(List.of("v2", "v1"));
        governance.setRollout(rollout);
        snapshot.setIngestionGovernance(governance);
        client.snapshot = snapshot;

        syncService.syncOnce(true);

        client.finalizeAck = true;
        snapshot.setVersion("v-retry-2");
        syncService.syncOnce(false);

        assertThat(client.finalizePushCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldSkipFinalizeRetryWhenReconcileMarksTaskAcked() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        client.finalizeAck = false;
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(true);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);

        SfChainIngestionProperties ingestionProperties = new SfChainIngestionProperties();
        ingestionProperties.setSupportedContractVersion("v1");
        ingestionProperties.setRequireCurrentVersionOverlap(false);
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(ingestionProperties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                ingestionProperties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );
        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                applier
        );

        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setReleaseId("release-reconcile");
        rollout.setStage("FULL");
        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion("v-reconcile-1");
        RemoteIngestionGovernanceSnapshot governance = new RemoteIngestionGovernanceSnapshot();
        governance.setContractAllowlist(List.of("v2", "v1"));
        governance.setRollout(rollout);
        snapshot.setIngestionGovernance(governance);
        client.snapshot = snapshot;
        syncService.syncOnce(true);

        GovernanceFinalizeReconcileSnapshot reconcile = new GovernanceFinalizeReconcileSnapshot();
        reconcile.setAckedTaskKeys(List.of("release-reconcile|SUCCEEDED"));
        client.reconcileSnapshot = reconcile;

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-reconcile-1");
        notModified.setNotModified(true);
        client.snapshot = notModified;
        syncService.syncOnce(false);

        assertThat(client.finalizePushCount).isEqualTo(1);
    }

    @Test
    void shouldRecordRemoteLeaseAndReconcileMetrics() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        client.leaseAcquireToken = "lease-token-1";
        client.reconcileSnapshot = new GovernanceFinalizeReconcileSnapshot();

        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceRemoteLeaseEnabled(true);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(false);
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(false);

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-metrics-1");
        notModified.setNotModified(true);
        client.snapshot = notModified;

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        syncService.syncOnce(false);

        GovernanceSyncMetricsSnapshot metrics = syncService.metrics();
        assertThat(metrics.getSyncRunCount()).isEqualTo(1);
        assertThat(metrics.getSyncFailureCount()).isEqualTo(0);
        assertThat(metrics.getLeaseAcquireAttempts()).isEqualTo(1);
        assertThat(metrics.getLeaseAcquireSuccess()).isEqualTo(1);
        assertThat(metrics.getLeaseAcquireRemoteSuccess()).isEqualTo(1);
        assertThat(metrics.getLeaseAcquireLocalSuccess()).isEqualTo(0);
        assertThat(metrics.getFinalizeReconcileAttempts()).isEqualTo(1);
        assertThat(metrics.getFinalizeReconcileSuccess()).isEqualTo(1);
        assertThat(metrics.getFinalizeReconcileFailure()).isEqualTo(0);
    }

    @Test
    void shouldAdvanceReconcileCursorAcrossSyncCycles() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceLeaseEnabled(false);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(false);
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(false);

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-cursor-1");
        notModified.setNotModified(true);
        client.snapshot = notModified;

        GovernanceFinalizeReconcileSnapshot first = new GovernanceFinalizeReconcileSnapshot();
        first.setNextCursor("cursor-1");
        GovernanceFinalizeReconcileSnapshot second = new GovernanceFinalizeReconcileSnapshot();
        second.setNextCursor("cursor-2");
        client.reconcileSnapshotByCursor.put("", first);
        client.reconcileSnapshotByCursor.put("cursor-1", second);

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        syncService.syncOnce(false);
        syncService.syncOnce(false);

        assertThat(client.reconcileCursorRequests).containsExactly("", "cursor-1");
    }

    @Test
    void shouldResumeReconcileCursorAfterServiceRestart() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        Path tempDir = Files.createTempDirectory("sf-chain-reconcile-restart");
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceLeaseEnabled(false);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);
        syncProperties.setIngestionGovernanceEnabled(false);
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(false);
        syncProperties.setGovernanceStateFile(tempDir.resolve("state.json").toString());
        syncProperties.setCacheFile(tempDir.resolve("cache.json").toString());

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-restart-1");
        notModified.setNotModified(true);

        StubRemoteConfigClient client1 = new StubRemoteConfigClient(objectMapper, serverProperties);
        client1.snapshot = notModified;
        GovernanceFinalizeReconcileSnapshot first = new GovernanceFinalizeReconcileSnapshot();
        first.setNextCursor("cursor-restart-1");
        client1.reconcileSnapshotByCursor.put("", first);

        RemoteConfigSyncService service1 = new RemoteConfigSyncService(
                client1,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        service1.start();
        service1.stop();

        StubRemoteConfigClient client2 = new StubRemoteConfigClient(objectMapper, serverProperties);
        client2.snapshot = notModified;
        GovernanceFinalizeReconcileSnapshot second = new GovernanceFinalizeReconcileSnapshot();
        second.setNextCursor("cursor-restart-2");
        client2.reconcileSnapshotByCursor.put("cursor-restart-1", second);

        RemoteConfigSyncService service2 = new RemoteConfigSyncService(
                client2,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        service2.start();
        service2.stop();

        assertThat(client2.reconcileCursorRequests).contains("cursor-restart-1");
    }

    @Test
    void shouldPullMultipleReconcilePagesInSingleSync() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceLeaseEnabled(false);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);
        syncProperties.setGovernanceFinalizeReconcileMaxPages(3);
        syncProperties.setIngestionGovernanceEnabled(false);
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(true);

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-page-1");
        notModified.setNotModified(true);
        client.snapshot = notModified;

        GovernanceFinalizeReconcileSnapshot page1 = new GovernanceFinalizeReconcileSnapshot();
        page1.setAckedTaskKeys(List.of("release-page|SUCCEEDED"));
        page1.setNextCursor("cursor-page-1");
        page1.setHasMore(true);
        GovernanceFinalizeReconcileSnapshot page2 = new GovernanceFinalizeReconcileSnapshot();
        page2.setNextCursor("cursor-page-2");
        page2.setHasMore(false);
        client.reconcileSnapshotByCursor.put("", page1);
        client.reconcileSnapshotByCursor.put("cursor-page-1", page2);

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        syncService.syncOnce(false);

        assertThat(client.reconcileCursorRequests).containsExactly("", "cursor-page-1");
        GovernanceSyncMetricsSnapshot metrics = syncService.metrics();
        assertThat(metrics.getFinalizeReconcileAttempts()).isEqualTo(2);
    }

    @Test
    void shouldResetCursorWhenReconcileCursorInvalid() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceLeaseEnabled(false);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);
        syncProperties.setGovernanceFinalizeReconcileMaxPages(3);
        syncProperties.setIngestionGovernanceEnabled(false);
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(false);

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-invalid-cursor-1");
        notModified.setNotModified(true);
        client.snapshot = notModified;

        GovernanceFinalizeReconcileSnapshot init = new GovernanceFinalizeReconcileSnapshot();
        init.setNextCursor("bad-cursor");
        init.setHasMore(false);
        client.reconcileSnapshotByCursor.put("", init);
        RemoteConfigSyncService firstService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        firstService.start();
        firstService.stop();

        GovernanceFinalizeReconcileSnapshot afterReset = new GovernanceFinalizeReconcileSnapshot();
        afterReset.setNextCursor("good-cursor");
        afterReset.setHasMore(false);
        client.reconcileSnapshotByCursor.put("", afterReset);
        client.invalidReconcileCursors.add("bad-cursor");

        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        syncService.start();
        syncService.stop();

        assertThat(client.reconcileCursorRequests).contains("bad-cursor", "");
        GovernanceSyncMetricsSnapshot metrics = syncService.metrics();
        assertThat(metrics.getFinalizeReconcileInvalidCursorCount()).isEqualTo(1);
        assertThat(metrics.getFinalizeReconcileCursorResetCount()).isEqualTo(1);
        assertThat(metrics.getFinalizeReconcileInvalidCursorFailFastCount()).isEqualTo(0);
    }

    @Test
    void shouldFailFastWhenInvalidCursorStrategyIsFailFast() {
        ObjectMapper objectMapper = new ObjectMapper();
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://localhost");
        serverProperties.setApiKey("token");

        StubRemoteConfigClient client = new StubRemoteConfigClient(objectMapper, serverProperties);
        SfChainConfigSyncProperties syncProperties = new SfChainConfigSyncProperties();
        syncProperties.setGovernanceLeaseEnabled(false);
        syncProperties.setGovernanceFinalizeReconcileEnabled(true);
        syncProperties.setGovernanceFinalizeReconcileMaxPages(3);
        syncProperties.setGovernanceFinalizeReconcileInvalidCursorStrategy(
                GovernanceInvalidCursorStrategy.FAIL_FAST);
        syncProperties.setIngestionGovernanceEnabled(false);
        syncProperties.setGovernanceFeedbackEnabled(false);
        syncProperties.setGovernanceEventEnabled(false);
        syncProperties.setGovernanceFinalizeEnabled(false);

        RemoteConfigSnapshot notModified = new RemoteConfigSnapshot();
        notModified.setVersion("v-invalid-fast-1");
        notModified.setNotModified(true);
        client.snapshot = notModified;

        GovernanceFinalizeReconcileSnapshot init = new GovernanceFinalizeReconcileSnapshot();
        init.setNextCursor("bad-fast-cursor");
        init.setHasMore(false);
        client.reconcileSnapshotByCursor.put("", init);
        RemoteConfigSyncService firstService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        firstService.start();
        firstService.stop();

        client.invalidReconcileCursors.add("bad-fast-cursor");
        RemoteConfigSyncService syncService = new RemoteConfigSyncService(
                client,
                syncProperties,
                new OpenAIModelFactory(),
                new AIOperationRegistry(),
                objectMapper,
                null
        );
        syncService.start();
        syncService.stop();

        assertThat(client.reconcileCursorRequests).contains("bad-fast-cursor");
        assertThat(client.reconcileCursorRequests.stream().filter("bad-fast-cursor"::equals).count()).isEqualTo(1);
        assertThat(client.reconcileCursorRequests.stream().filter(""::equals).count()).isEqualTo(1);
        GovernanceSyncMetricsSnapshot metrics = syncService.metrics();
        assertThat(metrics.getFinalizeReconcileInvalidCursorCount()).isEqualTo(1);
        assertThat(metrics.getFinalizeReconcileCursorResetCount()).isEqualTo(0);
        assertThat(metrics.getFinalizeReconcileInvalidCursorFailFastCount()).isEqualTo(1);
    }

    private static class StubRemoteConfigClient extends RemoteConfigClient {

        private RemoteConfigSnapshot snapshot;
        private String lastFeedbackVersion;
        private GovernanceSyncApplyResult lastFeedbackResult;
        private int eventPushCount;
        private int finalizePushCount;
        private boolean finalizeAck = true;
        private GovernanceFinalizeReconcileSnapshot reconcileSnapshot;
        private String leaseAcquireToken;
        private final java.util.Map<String, GovernanceFinalizeReconcileSnapshot> reconcileSnapshotByCursor = new java.util.HashMap<>();
        private final java.util.List<String> reconcileCursorRequests = new java.util.ArrayList<>();
        private final java.util.Set<String> invalidReconcileCursors = new java.util.HashSet<>();

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

        @Override
        public void pushGovernanceEvent(String snapshotVersion, GovernanceSyncApplyResult result) {
            this.eventPushCount++;
        }

        @Override
        public GovernanceFinalizeAck pushGovernanceFinalize(String snapshotVersion, GovernanceSyncApplyResult result) {
            this.finalizePushCount++;
            GovernanceFinalizeAck ack = new GovernanceFinalizeAck();
            ack.setAcknowledged(finalizeAck);
            ack.setAckId("ack-" + finalizePushCount);
            ack.setAckVersion((long) finalizePushCount);
            return ack;
        }

        @Override
        public Optional<GovernanceFinalizeReconcileSnapshot> fetchFinalizeReconciliation() {
            return Optional.ofNullable(reconcileSnapshot);
        }

        @Override
        public Optional<GovernanceFinalizeReconcileSnapshot> fetchFinalizeReconciliation(String cursor) {
            String normalized = cursor == null ? "" : cursor;
            reconcileCursorRequests.add(normalized);
            if (invalidReconcileCursors.contains(normalized)) {
                throw new InvalidReconcileCursorException("invalid cursor: " + normalized);
            }
            if (!reconcileSnapshotByCursor.isEmpty()) {
                return Optional.ofNullable(reconcileSnapshotByCursor.get(normalized));
            }
            return Optional.ofNullable(reconcileSnapshot);
        }

        @Override
        public Optional<String> tryAcquireGovernanceLease(String owner, int ttlSeconds) {
            return Optional.ofNullable(leaseAcquireToken);
        }
    }
}
