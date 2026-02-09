package com.suifeng.sfchain.config.remote;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthSnapshot;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 日志治理配置应用器
 */
@RequiredArgsConstructor
public class IngestionGovernanceSyncApplier {

    private final SfChainIngestionProperties ingestionProperties;
    private final ContractAllowlistGuardService guardService;
    private final IngestionIndexMaintenanceService indexMaintenanceService;
    private final IngestionContractHealthTracker contractHealthTracker;
    private final String localAppId;
    private volatile String failedReleaseId;
    private volatile long retryBlockedUntilEpochMs;
    private volatile long rollbackCooldownUntilEpochMs;
    private volatile String activeReleaseId;
    private volatile int activeReleasePriority;

    public synchronized GovernanceSyncApplyResult apply(RemoteIngestionGovernanceSnapshot snapshot) {
        GovernanceSyncApplyResult result = new GovernanceSyncApplyResult();
        if (snapshot == null) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setMessage("no governance payload");
            result.setStatus(GovernanceReleaseStatus.NOOP);
            result.setReasonCode("NO_PAYLOAD");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }

        RemoteGovernanceRolloutPlan rollout = snapshot.getRollout();
        if (rollout != null) {
            result.setReleaseId(rollout.getReleaseId());
            result.setStage(normalizeStage(rollout.getStage()));
        }

        long now = System.currentTimeMillis();
        result.setEventTimeEpochMs(now);
        if (isRollbackCoolingDown(now)) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setStatus(GovernanceReleaseStatus.COOLDOWN);
            result.setReasonCode("ROLLBACK_COOLDOWN");
            result.setNextRetryAtEpochMs(rollbackCooldownUntilEpochMs);
            result.setMessage("rollback cooldown active");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }
        if (isRetryBlocked(now, rollout == null ? null : rollout.getReleaseId())) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setStatus(GovernanceReleaseStatus.RETRY_WAIT);
            result.setReasonCode("RETRY_BACKOFF");
            result.setNextRetryAtEpochMs(retryBlockedUntilEpochMs);
            result.setMessage("retry backoff active");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }

        if (hasReleaseConflict(rollout)) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setStatus(GovernanceReleaseStatus.SKIPPED);
            result.setReasonCode("RELEASE_CONFLICT");
            result.setMessage("blocked by active release");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }

        List<String> requestedVersions = normalize(snapshot.getContractAllowlist());
        result.setRequestedVersions(requestedVersions);

        if (!isTargetedForApply(rollout)) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setStatus(GovernanceReleaseStatus.SKIPPED);
            result.setReasonCode("CANARY_SKIP");
            result.setMessage("not in canary cohort");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }
        result.setTargeted(true);
        enterReleaseIfNeeded(rollout);

        if (!requestedVersions.isEmpty()) {
            ContractAllowlistGuardService.ValidationResult validation = guardService.validate(requestedVersions);
            result.setValid(validation.isValid());
            result.setMessage(joinErrors(validation.getErrors()));
            if (!validation.isValid()) {
                result.setApplied(false);
                result.setStatus(GovernanceReleaseStatus.FAILED);
                result.setReasonCode("VALIDATION_FAILED");
                markReleaseFailed(rollout, now);
                clearReleaseIfTerminal(result.getStatus(), rollout);
                result.setActiveVersions(resolveActiveVersions());
                return result;
            }

            applyAllowlist(requestedVersions);
            result.setApplied(true);
            result.setStatus(isCanaryStage(rollout) ? GovernanceReleaseStatus.RUNNING : GovernanceReleaseStatus.SUCCEEDED);
            result.setReasonCode(isCanaryStage(rollout) ? "CANARY_APPLIED" : "FULL_APPLIED");
            clearReleaseFailure();
            clearReleaseIfTerminal(result.getStatus(), rollout);
        } else {
            result.setValid(true);
            result.setApplied(false);
            result.setStatus(GovernanceReleaseStatus.NOOP);
            result.setReasonCode("EMPTY_ALLOWLIST");
            result.setMessage("no allowlist update");
        }

        if (shouldRollback(rollout, result)) {
            List<String> rollbackVersions = normalize(rollout.getRollbackAllowlist());
            if (!rollbackVersions.isEmpty()) {
                applyAllowlist(rollbackVersions);
                result.setRolledBack(true);
                result.setApplied(false);
                result.setStatus(GovernanceReleaseStatus.ROLLED_BACK);
                result.setReasonCode("THRESHOLD_VIOLATION");
                result.setMessage("canary rollback triggered");
                markRollbackCooldown(rollout, now);
                markReleaseFailed(rollout, now);
                clearReleaseIfTerminal(result.getStatus(), rollout);
            } else {
                result.setStatus(GovernanceReleaseStatus.FAILED);
                result.setReasonCode("ROLLBACK_TARGET_MISSING");
                result.setMessage("rollback target missing");
                markReleaseFailed(rollout, now);
                clearReleaseIfTerminal(result.getStatus(), rollout);
            }
        }

        if (snapshot.isRebuildIndexes() && indexMaintenanceService != null) {
            result.setRebuilt(indexMaintenanceService.rebuildOnce());
        }
        result.setActiveVersions(resolveActiveVersions());
        return result;
    }

    private List<String> resolveActiveVersions() {
        List<String> versions = new ArrayList<>();
        String single = ingestionProperties.getSupportedContractVersion();
        if (single != null && !single.isBlank()) {
            versions.add(single.trim());
        }
        List<String> multiple = ingestionProperties.getSupportedContractVersions();
        if (multiple != null) {
            for (String item : multiple) {
                if (item != null && !item.isBlank()) {
                    versions.add(item.trim());
                }
            }
        }
        return normalize(versions);
    }

    private static List<String> normalize(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim());
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "ok";
        }
        StringJoiner joiner = new StringJoiner("; ");
        for (String error : errors) {
            if (error != null && !error.isBlank()) {
                joiner.add(error.trim());
            }
        }
        String message = joiner.toString();
        return message.isBlank() ? "invalid" : message;
    }

    private void applyAllowlist(List<String> versions) {
        ingestionProperties.setSupportedContractVersion(versions.get(0));
        ingestionProperties.setSupportedContractVersions(new ArrayList<>(versions));
    }

    private boolean isTargetedForApply(RemoteGovernanceRolloutPlan rollout) {
        if (!isCanaryStage(rollout)) {
            return true;
        }
        int percent = Math.max(0, Math.min(rollout.getTrafficPercent(), 100));
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        String appId = StringUtils.hasText(localAppId) ? localAppId : "default";
        int bucket = Math.floorMod(appId.hashCode(), 100);
        return bucket < percent;
    }

    private boolean shouldRollback(RemoteGovernanceRolloutPlan rollout, GovernanceSyncApplyResult result) {
        if (rollout == null || !rollout.isRollbackOnViolation()) {
            return false;
        }
        if (!isCanaryStage(rollout)) {
            return false;
        }
        if (contractHealthTracker == null) {
            return false;
        }
        IngestionContractHealthSnapshot health = contractHealthTracker.snapshot();
        result.setSampleCount(health.getTotalRequests());
        result.setRejectRate(health.getRejectRate());
        return health.getTotalRequests() >= Math.max(rollout.getMinSamples(), 1L)
                && health.getRejectRate() > Math.max(rollout.getMaxRejectRate(), 0D);
    }

    private static String normalizeStage(String stage) {
        return StringUtils.hasText(stage) ? stage.trim().toUpperCase() : "FULL";
    }

    private static boolean isCanaryStage(RemoteGovernanceRolloutPlan rollout) {
        return rollout != null && "CANARY".equalsIgnoreCase(normalizeStage(rollout.getStage()));
    }

    private boolean hasReleaseConflict(RemoteGovernanceRolloutPlan rollout) {
        if (!StringUtils.hasText(activeReleaseId) || rollout == null || !StringUtils.hasText(rollout.getReleaseId())) {
            return false;
        }
        String incomingReleaseId = rollout.getReleaseId().trim();
        if (incomingReleaseId.equals(activeReleaseId)) {
            return false;
        }
        return rollout.getPriority() <= activeReleasePriority;
    }

    private void enterReleaseIfNeeded(RemoteGovernanceRolloutPlan rollout) {
        if (rollout == null || !StringUtils.hasText(rollout.getReleaseId())) {
            return;
        }
        String incomingReleaseId = rollout.getReleaseId().trim();
        if (!incomingReleaseId.equals(activeReleaseId)) {
            activeReleaseId = incomingReleaseId;
            activeReleasePriority = rollout.getPriority();
        }
    }

    private boolean isRetryBlocked(long now, String releaseId) {
        return releaseId != null
                && releaseId.equals(failedReleaseId)
                && now < retryBlockedUntilEpochMs;
    }

    private boolean isRollbackCoolingDown(long now) {
        return now < rollbackCooldownUntilEpochMs;
    }

    private void clearReleaseFailure() {
        failedReleaseId = null;
        retryBlockedUntilEpochMs = 0L;
    }

    private void markReleaseFailed(RemoteGovernanceRolloutPlan rollout, long now) {
        if (rollout == null || !StringUtils.hasText(rollout.getReleaseId())) {
            return;
        }
        failedReleaseId = rollout.getReleaseId().trim();
        int backoffSeconds = Math.max(rollout.getRetryBackoffSeconds(), 0);
        retryBlockedUntilEpochMs = now + backoffSeconds * 1000L;
    }

    private void markRollbackCooldown(RemoteGovernanceRolloutPlan rollout, long now) {
        if (rollout == null) {
            return;
        }
        int cooldownSeconds = Math.max(rollout.getRollbackCooldownSeconds(), 0);
        rollbackCooldownUntilEpochMs = now + cooldownSeconds * 1000L;
    }

    private void clearReleaseIfTerminal(GovernanceReleaseStatus status, RemoteGovernanceRolloutPlan rollout) {
        if (rollout == null || !StringUtils.hasText(rollout.getReleaseId())) {
            return;
        }
        if (status == GovernanceReleaseStatus.SUCCEEDED
                || status == GovernanceReleaseStatus.FAILED
                || status == GovernanceReleaseStatus.ROLLED_BACK) {
            String releaseId = rollout.getReleaseId().trim();
            if (releaseId.equals(activeReleaseId)) {
                activeReleaseId = null;
                activeReleasePriority = 0;
            }
        }
    }
}
