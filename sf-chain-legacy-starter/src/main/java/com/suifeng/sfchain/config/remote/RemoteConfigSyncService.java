package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainConfigSyncProperties;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 远程配置同步服务
 */
@Slf4j
public class RemoteConfigSyncService {

    private final RemoteConfigClient remoteConfigClient;
    private final SfChainConfigSyncProperties syncProperties;
    private final OpenAIModelFactory modelFactory;
    private final AIOperationRegistry operationRegistry;
    private final ObjectMapper objectMapper;
    private final IngestionGovernanceSyncApplier governanceSyncApplier;
    private final ConcurrentMap<String, GovernanceReleaseStatus> finalizedReleaseStates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile String currentVersion;

    public RemoteConfigSyncService(
            RemoteConfigClient remoteConfigClient,
            SfChainConfigSyncProperties syncProperties,
            OpenAIModelFactory modelFactory,
            AIOperationRegistry operationRegistry,
            ObjectMapper objectMapper,
            IngestionGovernanceSyncApplier governanceSyncApplier) {
        this.remoteConfigClient = remoteConfigClient;
        this.syncProperties = syncProperties;
        this.modelFactory = modelFactory;
        this.operationRegistry = operationRegistry;
        this.objectMapper = objectMapper;
        this.governanceSyncApplier = governanceSyncApplier;
    }

    @PostConstruct
    public void start() {
        loadCachedSnapshot();
        syncOnce(true);

        int interval = Math.max(syncProperties.getIntervalSeconds(), 5);
        scheduler.scheduleWithFixedDelay(
                () -> syncOnce(false),
                interval,
                interval,
                TimeUnit.SECONDS
        );
        log.info("远程配置同步已启动, interval={}s", interval);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    void syncOnce(boolean startup) {
        try {
            Optional<RemoteConfigSnapshot> snapshotOpt = remoteConfigClient.fetchSnapshot(currentVersion);
            if (snapshotOpt.isEmpty()) {
                return;
            }
            RemoteConfigSnapshot snapshot = snapshotOpt.get();
            if (snapshot.isNotModified()) {
                return;
            }
            GovernanceSyncApplyResult governanceResult = applySnapshot(snapshot);
            persistSnapshot(snapshot);
            currentVersion = snapshot.getVersion();
            pushGovernanceFeedback(snapshot.getVersion(), governanceResult);
            pushGovernanceEvent(snapshot.getVersion(), governanceResult);
            pushGovernanceFinalize(snapshot.getVersion(), governanceResult);
            log.info("远程配置同步成功, version={}", currentVersion);
        } catch (Exception e) {
            if (!syncProperties.isFailOpen()) {
                throw new IllegalStateException("远程配置同步失败且 fail-open=false", e);
            }
            if (startup) {
                log.warn("启动阶段远程配置同步失败，已按 fail-open 继续: {}", e.getMessage());
            } else {
                log.warn("定时远程配置同步失败: {}", e.getMessage());
            }
        }
    }

    GovernanceSyncApplyResult applySnapshot(RemoteConfigSnapshot snapshot) {
        if (snapshot.getModels() != null) {
            snapshot.getModels().forEach(this::registerOrUpdateModel);
        }
        if (snapshot.getOperationConfigs() != null) {
            operationRegistry.setConfigs(new ConcurrentHashMap<>(snapshot.getOperationConfigs()));
        }
        if (snapshot.getOperationModelMapping() != null) {
            operationRegistry.setModelMapping(new ConcurrentHashMap<>(snapshot.getOperationModelMapping()));
        }
        if (!syncProperties.isIngestionGovernanceEnabled() || governanceSyncApplier == null) {
            return null;
        }
        return governanceSyncApplier.apply(snapshot.getIngestionGovernance());
    }

    private void registerOrUpdateModel(String modelName, OpenAIModelConfig config) {
        if (config == null) {
            return;
        }
        if (!StringUtils.hasText(config.getModelName())) {
            config.setModelName(modelName);
        }
        if (config.isValid() && Boolean.TRUE.equals(config.getEnabled())) {
            modelFactory.registerModel(config);
        }
    }

    private void loadCachedSnapshot() {
        Path cachePath = Paths.get(syncProperties.getCacheFile());
        if (!Files.exists(cachePath)) {
            return;
        }
        try {
            RemoteConfigSnapshot snapshot = objectMapper.readValue(Files.readString(cachePath), RemoteConfigSnapshot.class);
            if (snapshot != null) {
                applySnapshot(snapshot);
                currentVersion = snapshot.getVersion();
                log.info("已加载本地配置快照, version={}", currentVersion);
            }
        } catch (Exception e) {
            log.warn("加载本地配置快照失败: {}", e.getMessage());
        }
    }

    private void persistSnapshot(RemoteConfigSnapshot snapshot) throws IOException {
        Path cachePath = Paths.get(syncProperties.getCacheFile());
        Path parent = cachePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(cachePath, objectMapper.writeValueAsString(snapshot));
    }

    private void pushGovernanceFeedback(String snapshotVersion, GovernanceSyncApplyResult governanceResult) {
        if (!syncProperties.isGovernanceFeedbackEnabled() || governanceResult == null) {
            return;
        }
        try {
            remoteConfigClient.pushGovernanceFeedback(snapshotVersion, governanceResult);
        } catch (Exception ex) {
            log.warn("治理反馈上报失败: {}", ex.getMessage());
        }
    }

    private void pushGovernanceEvent(String snapshotVersion, GovernanceSyncApplyResult governanceResult) {
        if (!syncProperties.isGovernanceEventEnabled() || governanceResult == null) {
            return;
        }
        try {
            remoteConfigClient.pushGovernanceEvent(snapshotVersion, governanceResult);
        } catch (Exception ex) {
            log.warn("治理事件上报失败: {}", ex.getMessage());
        }
    }

    private void pushGovernanceFinalize(String snapshotVersion, GovernanceSyncApplyResult governanceResult) {
        if (!syncProperties.isGovernanceFinalizeEnabled() || governanceResult == null) {
            return;
        }
        if (!isTerminal(governanceResult.getStatus())) {
            return;
        }
        String releaseId = governanceResult.getReleaseId();
        if (!StringUtils.hasText(releaseId)) {
            return;
        }
        GovernanceReleaseStatus status = governanceResult.getStatus();
        String normalizedReleaseId = releaseId.trim();
        GovernanceReleaseStatus existing = finalizedReleaseStates.get(normalizedReleaseId);
        if (existing == status) {
            return;
        }
        finalizedReleaseStates.put(normalizedReleaseId, status);
        try {
            remoteConfigClient.pushGovernanceFinalize(snapshotVersion, governanceResult);
        } catch (Exception ex) {
            log.warn("治理终态回调失败: {}", ex.getMessage());
        }
    }

    private static boolean isTerminal(GovernanceReleaseStatus status) {
        return status == GovernanceReleaseStatus.SUCCEEDED
                || status == GovernanceReleaseStatus.FAILED
                || status == GovernanceReleaseStatus.ROLLED_BACK;
    }
}
