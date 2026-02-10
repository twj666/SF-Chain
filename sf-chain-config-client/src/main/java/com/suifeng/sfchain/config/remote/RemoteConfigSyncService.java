package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.config.SfChainConfigSyncProperties;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.BaseAIOperation;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    private final GovernanceSyncApplier governanceSyncApplier;
    private final GovernanceSyncStateStore stateStore;
    private final GovernanceLeaseManager leaseManager;
    private final ConcurrentMap<String, GovernanceFinalizeRecord> finalizedReleaseStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, GovernanceFinalizeTask> pendingFinalizations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong syncRunCount = new AtomicLong();
    private final AtomicLong syncFailureCount = new AtomicLong();
    private final AtomicLong leaseAcquireAttempts = new AtomicLong();
    private final AtomicLong leaseAcquireSuccess = new AtomicLong();
    private final AtomicLong leaseAcquireRemoteSuccess = new AtomicLong();
    private final AtomicLong leaseAcquireLocalSuccess = new AtomicLong();
    private final AtomicLong finalizeReconcileAttempts = new AtomicLong();
    private final AtomicLong finalizeReconcileSuccess = new AtomicLong();
    private final AtomicLong finalizeReconcileFailure = new AtomicLong();
    private final AtomicLong finalizeReconcileInvalidCursorCount = new AtomicLong();
    private final AtomicLong finalizeReconcileCursorResetCount = new AtomicLong();
    private final AtomicLong finalizeReconcileInvalidCursorFailFastCount = new AtomicLong();
    private final AtomicLong finalizeRetryAttempts = new AtomicLong();
    private final AtomicLong finalizeRetrySuccess = new AtomicLong();
    private final AtomicLong finalizeRetryFailure = new AtomicLong();
    private volatile String currentVersion;
    private volatile String reconcileCursor;
    private volatile RemoteConfigSnapshot lastAppliedSnapshot;

    public RemoteConfigSyncService(
            RemoteConfigClient remoteConfigClient,
            SfChainConfigSyncProperties syncProperties,
            OpenAIModelFactory modelFactory,
            AIOperationRegistry operationRegistry,
            ObjectMapper objectMapper,
            GovernanceSyncApplier governanceSyncApplier) {
        this.remoteConfigClient = remoteConfigClient;
        this.syncProperties = syncProperties;
        this.modelFactory = modelFactory;
        this.operationRegistry = operationRegistry;
        this.objectMapper = objectMapper;
        this.governanceSyncApplier = governanceSyncApplier;
        this.stateStore = new GovernanceSyncStateStore(objectMapper, syncProperties.getGovernanceStateFile());
        this.leaseManager = new GovernanceLeaseManager(
                syncProperties.isGovernanceLeaseEnabled(),
                syncProperties.isGovernanceRemoteLeaseEnabled(),
                syncProperties.getGovernanceRemoteLeaseTtlSeconds(),
                "sf-chain:" + syncProperties.getGovernanceLeaseFile(),
                remoteConfigClient,
                syncProperties.getGovernanceLeaseFile()
        );
    }

    @PostConstruct
    public void start() {
        loadRuntimeState();
        loadCachedSnapshot();
        syncOperationCatalogAtStartup();
        if (syncProperties.isStartupCheckEnabled()) {
            log.info("启动阶段远程配置严格检查已开启, maxAttempts={}, retryIntervalMs={}",
                    Math.max(syncProperties.getStartupMaxAttempts(), 1),
                    Math.max(syncProperties.getStartupRetryIntervalMs(), 200L));
            runStartupSyncWithRetry();
        } else {
            log.info("启动阶段远程配置严格检查已关闭, 失败时按 fail-open={} 处理", syncProperties.isFailOpen());
            syncOnce(true);
        }

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
        leaseManager.release();
    }

    void syncOnce(boolean startup) {
        syncRunCount.incrementAndGet();
        leaseAcquireAttempts.incrementAndGet();
        boolean leaseAcquired = leaseManager.tryAcquire();
        if (leaseAcquired) {
            leaseAcquireSuccess.incrementAndGet();
            GovernanceLeaseManager.AcquireMode mode = leaseManager.getLastAcquireMode();
            if (mode == GovernanceLeaseManager.AcquireMode.REMOTE) {
                leaseAcquireRemoteSuccess.incrementAndGet();
            } else if (mode == GovernanceLeaseManager.AcquireMode.LOCAL) {
                leaseAcquireLocalSuccess.incrementAndGet();
            }
        }
        try {
            if (leaseAcquired) {
                reconcileFinalizeState();
                flushPendingFinalizations();
            }
            Optional<RemoteConfigSnapshot> snapshotOpt = remoteConfigClient.fetchSnapshot(currentVersion);
            if (snapshotOpt.isEmpty()) {
                if (leaseAcquired) {
                    compactRuntimeState();
                    persistRuntimeState();
                }
                return;
            }
            RemoteConfigSnapshot snapshot = snapshotOpt.get();
            if (snapshot.isNotModified()) {
                if (leaseAcquired) {
                    compactRuntimeState();
                    persistRuntimeState();
                }
                log.debug("远程配置未变更, version={}", currentVersion);
                return;
            }
            logSnapshotDiff(lastAppliedSnapshot, snapshot);
            GovernanceSyncApplyResult governanceResult = applySnapshot(snapshot, leaseAcquired);
            persistSnapshot(snapshot);
            currentVersion = snapshot.getVersion();
            lastAppliedSnapshot = snapshot;
            if (leaseAcquired) {
                pushGovernanceFeedback(snapshot.getVersion(), governanceResult);
                pushGovernanceEvent(snapshot.getVersion(), governanceResult);
                pushGovernanceFinalize(snapshot.getVersion(), governanceResult);
                flushPendingFinalizations();
            }
            compactRuntimeState();
            persistRuntimeState();
        } catch (Exception e) {
            syncFailureCount.incrementAndGet();
            if (startup && syncProperties.isStartupCheckEnabled()) {
                throw new IllegalStateException("启动阶段远程配置同步失败", e);
            }
            if (!syncProperties.isFailOpen()) {
                throw new IllegalStateException("远程配置同步失败且 fail-open=false", e);
            }
            if (startup) {
                log.warn("启动阶段远程配置同步失败，已按 fail-open 继续: {}", e.getMessage());
            } else {
                log.warn("定时远程配置同步失败: {}", e.getMessage());
            }
            persistRuntimeState();
        } finally {
            leaseManager.release();
        }
    }

    private void syncOperationCatalogAtStartup() {
        if (!syncProperties.isOperationCatalogSyncEnabled()) {
            return;
        }
        List<RemoteConfigClient.OperationCatalogItem> items = buildOperationCatalog();
        if (items.isEmpty()) {
            log.info("Skip operation catalog sync: no available AIOp operations");
            return;
        }
        try {
            remoteConfigClient.pushOperationCatalog(items);
            log.info("Operation catalog sync completed: count={}", items.size());
        } catch (Exception ex) {
            if (!syncProperties.isFailOpen()) {
                throw new IllegalStateException("Operation catalog sync failed and fail-open=false", ex);
            }
            log.warn("Operation catalog sync failed, continue because fail-open=true: {}", ex.getMessage());
        }
    }

    private List<RemoteConfigClient.OperationCatalogItem> buildOperationCatalog() {
        List<String> operationTypes = new ArrayList<>(operationRegistry.getAllOperations());
        operationTypes.sort(String::compareTo);
        List<RemoteConfigClient.OperationCatalogItem> items = new ArrayList<>(operationTypes.size());
        for (String operationType : operationTypes) {
            if (!StringUtils.hasText(operationType)) {
                continue;
            }
            try {
                BaseAIOperation<?, ?> operation = operationRegistry.getOperation(operationType);
                if (operation == null) {
                    continue;
                }
                AIOp annotation = operation.getAnnotation();
                RemoteConfigClient.OperationCatalogItem item = new RemoteConfigClient.OperationCatalogItem();
                item.setOperationType(operationType);
                item.setSourceClass(operation.getClass().getName());
                if (annotation != null) {
                    item.setDescription(annotation.description());
                    item.setDefaultModel(annotation.defaultModel());
                    item.setEnabled(annotation.enabled());
                    item.setRequireJsonOutput(annotation.requireJsonOutput());
                    item.setSupportThinking(annotation.supportThinking());
                    item.setDefaultMaxTokens(annotation.defaultMaxTokens());
                    item.setDefaultTemperature(annotation.defaultTemperature());
                    String[] supportedModels = annotation.supportedModels();
                    if (supportedModels != null && supportedModels.length > 0) {
                        item.setSupportedModels(List.of(supportedModels));
                    }
                }
                items.add(item);
            } catch (Exception ex) {
                log.warn("Failed to build operation catalog item, operationType={}, err={}", operationType, ex.getMessage());
            }
        }
        return items;
    }

    GovernanceSyncApplyResult applySnapshot(RemoteConfigSnapshot snapshot, boolean leaseAcquired) {
        if (snapshot.getModels() != null) {
            snapshot.getModels().forEach(this::registerOrUpdateModel);
        }
        if (snapshot.getOperationConfigs() != null) {
            operationRegistry.setConfigs(new ConcurrentHashMap<>(snapshot.getOperationConfigs()));
        }
        if (snapshot.getOperationModelMapping() != null) {
            operationRegistry.setModelMapping(new ConcurrentHashMap<>(snapshot.getOperationModelMapping()));
        }
        if (!syncProperties.isIngestionGovernanceEnabled() || governanceSyncApplier == null || !leaseAcquired) {
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
                applySnapshot(snapshot, false);
                currentVersion = snapshot.getVersion();
                lastAppliedSnapshot = snapshot;
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
        GovernanceFinalizeRecord existing = finalizedReleaseStates.get(normalizedReleaseId);
        if (existing != null && existing.getStatus() == status && existing.getAckId() != null) {
            return;
        }
        GovernanceFinalizeRecord record = existing == null ? new GovernanceFinalizeRecord() : existing;
        record.setStatus(status);
        record.setUpdatedAtEpochMs(System.currentTimeMillis());
        finalizedReleaseStates.put(normalizedReleaseId, record);
        GovernanceFinalizeTask task = new GovernanceFinalizeTask();
        task.setSnapshotVersion(snapshotVersion);
        task.setResult(governanceResult);
        task.setUpdatedAtEpochMs(System.currentTimeMillis());
        pendingFinalizations.put(buildFinalizeTaskKey(normalizedReleaseId, status), task);
        try {
            GovernanceFinalizeAck ack = remoteConfigClient.pushGovernanceFinalize(snapshotVersion, governanceResult);
            onFinalizeAck(normalizedReleaseId, status, buildFinalizeTaskKey(normalizedReleaseId, status), task, ack);
        } catch (Exception ex) {
            log.warn("治理终态回调失败: {}", ex.getMessage());
        }
    }

    private static boolean isTerminal(GovernanceReleaseStatus status) {
        return status == GovernanceReleaseStatus.SUCCEEDED
                || status == GovernanceReleaseStatus.FAILED
                || status == GovernanceReleaseStatus.ROLLED_BACK;
    }

    private void flushPendingFinalizations() {
        if (!syncProperties.isGovernanceFinalizeEnabled() || pendingFinalizations.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : pendingFinalizations.keySet()) {
            GovernanceFinalizeTask task = pendingFinalizations.get(key);
            if (task == null || task.getResult() == null) {
                continue;
            }
            if (task.getNextAttemptAtEpochMs() > now) {
                continue;
            }
            GovernanceReleaseStatus status = task.getResult().getStatus();
            String releaseId = task.getResult().getReleaseId();
            if (!StringUtils.hasText(releaseId) || status == null) {
                continue;
            }
            String normalizedReleaseId = releaseId.trim();
            try {
                finalizeRetryAttempts.incrementAndGet();
                GovernanceFinalizeAck ack = remoteConfigClient.pushGovernanceFinalize(task.getSnapshotVersion(), task.getResult());
                if (ack != null && ack.isAcknowledged()) {
                    finalizeRetrySuccess.incrementAndGet();
                } else {
                    finalizeRetryFailure.incrementAndGet();
                }
                onFinalizeAck(normalizedReleaseId, status, key, task, ack);
            } catch (Exception ex) {
                log.warn("重试治理终态回调失败: {}", ex.getMessage());
                finalizeRetryFailure.incrementAndGet();
                scheduleFinalizeRetry(task);
            }
        }
    }

    private void reconcileFinalizeState() {
        if (!syncProperties.isGovernanceFinalizeReconcileEnabled()) {
            return;
        }
        int maxPages = Math.max(syncProperties.getGovernanceFinalizeReconcileMaxPages(), 1);
        String cursor = reconcileCursor;
        boolean cursorResetAttempted = false;
        for (int page = 0; page < maxPages; page++) {
            finalizeReconcileAttempts.incrementAndGet();
            try {
                Optional<GovernanceFinalizeReconcileSnapshot> snapshotOpt =
                        remoteConfigClient.fetchFinalizeReconciliation(cursor);
                if (snapshotOpt.isEmpty()) {
                    finalizeReconcileSuccess.incrementAndGet();
                    break;
                }
                GovernanceFinalizeReconcileSnapshot snapshot = snapshotOpt.get();
                if (snapshot.getAckedTaskKeys() != null) {
                    for (String key : snapshot.getAckedTaskKeys()) {
                        if (key != null && !key.isBlank()) {
                            pendingFinalizations.remove(key.trim());
                        }
                    }
                }
                finalizeReconcileSuccess.incrementAndGet();
                String nextCursor = StringUtils.hasText(snapshot.getNextCursor())
                        ? snapshot.getNextCursor().trim()
                        : null;
                if (nextCursor != null) {
                    reconcileCursor = nextCursor;
                }
                if (!snapshot.isHasMore() || !StringUtils.hasText(nextCursor) || nextCursor.equals(cursor)) {
                    break;
                }
                cursor = nextCursor;
                cursorResetAttempted = false;
            } catch (InvalidReconcileCursorException ex) {
                finalizeReconcileFailure.incrementAndGet();
                long invalidCount = finalizeReconcileInvalidCursorCount.incrementAndGet();
                warnIfInvalidCursorThresholdReached(invalidCount);
                GovernanceInvalidCursorStrategy strategy =
                        syncProperties.getGovernanceFinalizeReconcileInvalidCursorStrategy();
                if (strategy == GovernanceInvalidCursorStrategy.FAIL_FAST) {
                    finalizeReconcileInvalidCursorFailFastCount.incrementAndGet();
                    log.warn("治理finalize对账游标无效，按FAIL_FAST策略中断本轮对账: {}", ex.getMessage());
                    break;
                }
                if (cursorResetAttempted) {
                    log.warn("治理finalize对账游标重置后仍无效: {}", ex.getMessage());
                    break;
                }
                reconcileCursor = null;
                cursor = null;
                finalizeReconcileCursorResetCount.incrementAndGet();
                cursorResetAttempted = true;
            } catch (Exception ex) {
                finalizeReconcileFailure.incrementAndGet();
                log.warn("治理finalize对账拉取失败: {}", ex.getMessage());
                break;
            }
        }
    }

    private void loadRuntimeState() {
        stateStore.load().ifPresent(state -> {
            if (governanceSyncApplier != null) {
                governanceSyncApplier.restoreState(state.getApplierState());
            }
            if (state.getFinalizedStates() != null) {
                finalizedReleaseStates.putAll(state.getFinalizedStates());
            }
            if (state.getPendingFinalizations() != null) {
                pendingFinalizations.putAll(state.getPendingFinalizations());
            }
            if (StringUtils.hasText(state.getReconcileCursor())) {
                reconcileCursor = state.getReconcileCursor().trim();
            }
        });
    }

    private void persistRuntimeState() {
        GovernanceSyncRuntimeState state = new GovernanceSyncRuntimeState();
        if (governanceSyncApplier != null) {
            state.setApplierState(governanceSyncApplier.snapshotState());
        }
        state.setFinalizedStates(new LinkedHashMap<>(finalizedReleaseStates));
        state.setPendingFinalizations(new LinkedHashMap<>(pendingFinalizations));
        state.setReconcileCursor(reconcileCursor);
        stateStore.save(state);
    }

    private static String buildFinalizeTaskKey(String releaseId, GovernanceReleaseStatus status) {
        return releaseId + "|" + status.name();
    }

    private void onFinalizeAck(
            String releaseId,
            GovernanceReleaseStatus status,
            String taskKey,
            GovernanceFinalizeTask task,
            GovernanceFinalizeAck ack) {
        if (ack != null && ack.isAcknowledged()) {
            pendingFinalizations.remove(taskKey);
            GovernanceFinalizeRecord record = finalizedReleaseStates.get(releaseId);
            if (record == null) {
                record = new GovernanceFinalizeRecord();
                record.setStatus(status);
            }
            record.setUpdatedAtEpochMs(System.currentTimeMillis());
            record.setAckId(ack.getAckId());
            record.setAckVersion(ack.getAckVersion());
            finalizedReleaseStates.put(releaseId, record);
        } else {
            scheduleFinalizeRetry(task);
        }
    }

    private void scheduleFinalizeRetry(GovernanceFinalizeTask task) {
        int interval = Math.max(syncProperties.getGovernanceFinalizeRetrySeconds(), 1);
        task.setRetryCount(task.getRetryCount() + 1);
        task.setUpdatedAtEpochMs(System.currentTimeMillis());
        task.setNextAttemptAtEpochMs(System.currentTimeMillis() + interval * 1000L);
    }

    private void compactRuntimeState() {
        compactMapByUpdatedAt(finalizedReleaseStates, Math.max(syncProperties.getGovernanceStateMaxFinalized(), 1));
        compactMapByUpdatedAt(pendingFinalizations, Math.max(syncProperties.getGovernanceStateMaxPending(), 1));
    }

    private static <T> void compactMapByUpdatedAt(ConcurrentMap<String, T> map, int maxSize) {
        if (map.size() <= maxSize) {
            return;
        }
        java.util.List<java.util.Map.Entry<String, T>> entries = new java.util.ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Long.compare(resolveUpdatedAt(a.getValue()), resolveUpdatedAt(b.getValue())));
        int removeCount = map.size() - maxSize;
        for (int i = 0; i < removeCount && i < entries.size(); i++) {
            map.remove(entries.get(i).getKey());
        }
    }

    private static long resolveUpdatedAt(Object value) {
        if (value instanceof GovernanceFinalizeRecord) {
            return ((GovernanceFinalizeRecord) value).getUpdatedAtEpochMs();
        }
        if (value instanceof GovernanceFinalizeTask) {
            return ((GovernanceFinalizeTask) value).getUpdatedAtEpochMs();
        }
        return 0L;
    }

    public GovernanceSyncMetricsSnapshot metrics() {
        return GovernanceSyncMetricsSnapshot.builder()
                .syncRunCount(syncRunCount.get())
                .syncFailureCount(syncFailureCount.get())
                .leaseAcquireAttempts(leaseAcquireAttempts.get())
                .leaseAcquireSuccess(leaseAcquireSuccess.get())
                .leaseAcquireRemoteSuccess(leaseAcquireRemoteSuccess.get())
                .leaseAcquireLocalSuccess(leaseAcquireLocalSuccess.get())
                .finalizeReconcileAttempts(finalizeReconcileAttempts.get())
                .finalizeReconcileSuccess(finalizeReconcileSuccess.get())
                .finalizeReconcileFailure(finalizeReconcileFailure.get())
                .finalizeReconcileInvalidCursorCount(finalizeReconcileInvalidCursorCount.get())
                .finalizeReconcileCursorResetCount(finalizeReconcileCursorResetCount.get())
                .finalizeReconcileInvalidCursorFailFastCount(finalizeReconcileInvalidCursorFailFastCount.get())
                .finalizeRetryAttempts(finalizeRetryAttempts.get())
                .finalizeRetrySuccess(finalizeRetrySuccess.get())
                .finalizeRetryFailure(finalizeRetryFailure.get())
                .build();
    }

    private void warnIfInvalidCursorThresholdReached(long invalidCount) {
        int threshold = Math.max(syncProperties.getGovernanceFinalizeReconcileInvalidCursorWarnThreshold(), 1);
        if (invalidCount % threshold == 0) {
            log.warn("治理finalize对账无效游标累计达到阈值: count={}, threshold={}", invalidCount, threshold);
        }
    }

    private void runStartupSyncWithRetry() {
        int maxAttempts = Math.max(syncProperties.getStartupMaxAttempts(), 1);
        long retryIntervalMs = Math.max(syncProperties.getStartupRetryIntervalMs(), 200L);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                syncOnce(true);
                if (attempt > 1) {
                    log.info("启动阶段远程配置同步重试成功, attempt={}/{}", attempt, maxAttempts);
                }
                return;
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    throw new IllegalStateException(
                            "启动阶段远程配置同步失败且已达到最大重试次数, attempts=" + maxAttempts, ex);
                }
                log.warn("启动阶段远程配置同步失败, attempt={}/{}, {}ms后重试, err={}",
                        attempt, maxAttempts, retryIntervalMs, ex.getMessage());
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("启动阶段远程配置同步重试被中断", interruptedException);
                }
            }
        }
    }

    private void logSnapshotDiff(RemoteConfigSnapshot previous, RemoteConfigSnapshot current) {
        if (current == null) {
            return;
        }
        if (previous == null) {
            log.info("远程配置首轮加载: version={}, models={}, mappings={}, operationConfigs={}",
                    current.getVersion(),
                    sizeOf(current.getModels()),
                    sizeOf(current.getOperationModelMapping()),
                    sizeOf(current.getOperationConfigs()));
            return;
        }
        DiffResult modelDiff = diffKeyed(previous.getModels(), current.getModels());
        DiffResult mappingDiff = diffKeyed(previous.getOperationModelMapping(), current.getOperationModelMapping());
        DiffResult configDiff = diffKeyed(previous.getOperationConfigs(), current.getOperationConfigs());
        List<String> operationConfigFieldDiffs = diffOperationConfigFields(
                previous.getOperationConfigs(), current.getOperationConfigs());
        log.info("远程配置变更: {} -> {} | models(+{} -{} ~{}) mappings(+{} -{} ~{}) operationConfigs(+{} -{} ~{})",
                previous.getVersion(),
                current.getVersion(),
                modelDiff.added, modelDiff.removed, modelDiff.changed,
                mappingDiff.added, mappingDiff.removed, mappingDiff.changed,
                configDiff.added, configDiff.removed, configDiff.changed);
        if (!modelDiff.sampleChanges.isEmpty()) {
            log.info("模型变更详情: {}", modelDiff.sampleChanges);
        }
        if (!mappingDiff.sampleChanges.isEmpty()) {
            log.info("映射变更详情: {}", mappingDiff.sampleChanges);
        }
        if (!configDiff.sampleChanges.isEmpty()) {
            log.info("操作配置Key变更详情: {}", configDiff.sampleChanges);
        }
        if (!operationConfigFieldDiffs.isEmpty()) {
            log.info("操作配置字段变更详情: {}", operationConfigFieldDiffs);
        }
    }

    private static int sizeOf(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    private DiffResult diffKeyed(Map<?, ?> previous, Map<?, ?> current) {
        Map<?, ?> before = previous == null ? Map.of() : previous;
        Map<?, ?> after = current == null ? Map.of() : current;
        Set<Object> beforeKeys = new HashSet<>(before.keySet());
        Set<Object> afterKeys = new HashSet<>(after.keySet());

        int added = 0;
        int removed = 0;
        int changed = 0;
        List<String> sampleChanges = new ArrayList<>();

        for (Object key : afterKeys) {
            if (!beforeKeys.contains(key)) {
                added++;
                if (sampleChanges.size() < 8) {
                    sampleChanges.add("+" + key);
                }
            }
        }
        for (Object key : beforeKeys) {
            if (!afterKeys.contains(key)) {
                removed++;
                if (sampleChanges.size() < 8) {
                    sampleChanges.add("-" + key);
                }
            }
        }
        for (Object key : beforeKeys) {
            if (!afterKeys.contains(key)) {
                continue;
            }
            Object beforeValue = before.get(key);
            Object afterValue = after.get(key);
            if (!areEquivalent(beforeValue, afterValue)) {
                changed++;
                if (sampleChanges.size() < 8) {
                    sampleChanges.add("~" + key);
                }
            }
        }
        return new DiffResult(added, removed, changed, sampleChanges);
    }

    private boolean areEquivalent(Object beforeValue, Object afterValue) {
        if (beforeValue == afterValue) {
            return true;
        }
        if (beforeValue == null || afterValue == null) {
            return false;
        }
        JsonNode left = objectMapper.valueToTree(beforeValue);
        JsonNode right = objectMapper.valueToTree(afterValue);
        return left.equals(right);
    }

    private List<String> diffOperationConfigFields(
            Map<String, AIOperationRegistry.OperationConfig> previous,
            Map<String, AIOperationRegistry.OperationConfig> current) {
        Map<String, AIOperationRegistry.OperationConfig> before = previous == null ? Map.of() : previous;
        Map<String, AIOperationRegistry.OperationConfig> after = current == null ? Map.of() : current;
        List<String> details = new ArrayList<>();
        for (String operationType : before.keySet()) {
            if (!after.containsKey(operationType)) {
                continue;
            }
            AIOperationRegistry.OperationConfig beforeConfig = before.get(operationType);
            AIOperationRegistry.OperationConfig afterConfig = after.get(operationType);
            if (areEquivalent(beforeConfig, afterConfig)) {
                continue;
            }
            JsonNode beforeNode = objectMapper.valueToTree(beforeConfig);
            JsonNode afterNode = objectMapper.valueToTree(afterConfig);
            Set<String> fields = new java.util.TreeSet<>();
            beforeNode.fieldNames().forEachRemaining(fields::add);
            afterNode.fieldNames().forEachRemaining(fields::add);
            List<String> changedFields = new ArrayList<>();
            for (String field : fields) {
                JsonNode beforeField = beforeNode.get(field);
                JsonNode afterField = afterNode.get(field);
                if (beforeField == null && afterField == null) {
                    continue;
                }
                if (beforeField == null || afterField == null || !beforeField.equals(afterField)) {
                    changedFields.add(field + ":" + String.valueOf(beforeField) + "->" + String.valueOf(afterField));
                }
            }
            if (!changedFields.isEmpty()) {
                details.add(operationType + "{" + String.join(", ", changedFields) + "}");
                if (details.size() >= 8) {
                    break;
                }
            }
        }
        return details;
    }

    private static class DiffResult {
        private final int added;
        private final int removed;
        private final int changed;
        private final List<String> sampleChanges;

        private DiffResult(int added, int removed, int changed, List<String> sampleChanges) {
            this.added = added;
            this.removed = removed;
            this.changed = changed;
            this.sampleChanges = sampleChanges;
        }
    }
}
