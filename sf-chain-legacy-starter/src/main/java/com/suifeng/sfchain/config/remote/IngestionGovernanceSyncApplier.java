package com.suifeng.sfchain.config.remote;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import lombok.RequiredArgsConstructor;

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

    public GovernanceSyncApplyResult apply(RemoteIngestionGovernanceSnapshot snapshot) {
        GovernanceSyncApplyResult result = new GovernanceSyncApplyResult();
        if (snapshot == null) {
            result.setValid(true);
            result.setApplied(false);
            result.setMessage("no governance payload");
            result.setActiveVersions(resolveActiveVersions());
            return result;
        }

        List<String> requestedVersions = normalize(snapshot.getContractAllowlist());
        result.setRequestedVersions(requestedVersions);

        if (!requestedVersions.isEmpty()) {
            ContractAllowlistGuardService.ValidationResult validation = guardService.validate(requestedVersions);
            result.setValid(validation.isValid());
            result.setMessage(joinErrors(validation.getErrors()));
            if (!validation.isValid()) {
                result.setApplied(false);
                result.setActiveVersions(resolveActiveVersions());
                return result;
            }

            ingestionProperties.setSupportedContractVersion(requestedVersions.get(0));
            ingestionProperties.setSupportedContractVersions(new ArrayList<>(requestedVersions));
            result.setApplied(true);
        } else {
            result.setValid(true);
            result.setApplied(false);
            result.setMessage("no allowlist update");
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
}
