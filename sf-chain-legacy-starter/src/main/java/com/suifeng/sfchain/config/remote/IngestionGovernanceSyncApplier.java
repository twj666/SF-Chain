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

    public GovernanceSyncApplyResult apply(RemoteIngestionGovernanceSnapshot snapshot) {
        GovernanceSyncApplyResult result = new GovernanceSyncApplyResult();
        if (snapshot == null) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setMessage("no governance payload");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }

        RemoteGovernanceRolloutPlan rollout = snapshot.getRollout();
        if (rollout != null) {
            result.setReleaseId(rollout.getReleaseId());
            result.setStage(normalizeStage(rollout.getStage()));
        }

        List<String> requestedVersions = normalize(snapshot.getContractAllowlist());
        result.setRequestedVersions(requestedVersions);

        if (!isTargetedForApply(rollout)) {
            result.setValid(true);
            result.setApplied(false);
            result.setTargeted(false);
            result.setMessage("not in canary cohort");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }
        result.setTargeted(true);

        if (!requestedVersions.isEmpty()) {
            ContractAllowlistGuardService.ValidationResult validation = guardService.validate(requestedVersions);
            result.setValid(validation.isValid());
            result.setMessage(joinErrors(validation.getErrors()));
            if (!validation.isValid()) {
                result.setApplied(false);
                result.setActiveVersions(resolveActiveVersions());
                return result;
            }

            applyAllowlist(requestedVersions);
            result.setApplied(true);
        } else {
            result.setValid(true);
            result.setApplied(false);
            result.setMessage("no allowlist update");
        }

        if (shouldRollback(rollout, result)) {
            List<String> rollbackVersions = normalize(rollout.getRollbackAllowlist());
            if (!rollbackVersions.isEmpty()) {
                applyAllowlist(rollbackVersions);
                result.setRolledBack(true);
                result.setApplied(false);
                result.setMessage("canary rollback triggered");
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
        if (rollout == null || !"CANARY".equalsIgnoreCase(normalizeStage(rollout.getStage()))) {
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
        if (!"CANARY".equalsIgnoreCase(normalizeStage(rollout.getStage()))) {
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
}
