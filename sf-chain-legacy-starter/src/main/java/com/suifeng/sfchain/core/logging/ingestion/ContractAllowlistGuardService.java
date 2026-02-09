package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 契约白名单治理校验
 */
@RequiredArgsConstructor
public class ContractAllowlistGuardService {

    private final SfChainIngestionProperties properties;

    public ValidationResult validate(List<String> proposedVersions) {
        Set<String> current = normalize(currentAllowlist());
        Set<String> proposed = normalize(proposedVersions);
        List<String> errors = new ArrayList<>();

        if (proposed.isEmpty()) {
            errors.add("proposed versions must not be empty");
        }
        int maxActive = Math.max(properties.getMaxActiveContractVersions(), 1);
        if (proposed.size() > maxActive) {
            errors.add("proposed versions exceed max active limit");
        }
        if (properties.isRequireCurrentVersionOverlap() && !hasOverlap(current, proposed)) {
            errors.add("proposed versions must overlap with current allowlist");
        }

        return new ValidationResult(errors.isEmpty(), new ArrayList<>(current), new ArrayList<>(proposed), errors);
    }

    private List<String> currentAllowlist() {
        List<String> all = new ArrayList<>();
        if (properties.getSupportedContractVersion() != null) {
            all.add(properties.getSupportedContractVersion());
        }
        if (properties.getSupportedContractVersions() != null) {
            all.addAll(properties.getSupportedContractVersions());
        }
        return all;
    }

    private static Set<String> normalize(List<String> input) {
        Set<String> out = new LinkedHashSet<>();
        if (input == null) {
            return out;
        }
        for (String item : input) {
            if (item != null && !item.isBlank()) {
                out.add(item.trim());
            }
        }
        return out;
    }

    private static boolean hasOverlap(Set<String> a, Set<String> b) {
        for (String item : a) {
            if (b.contains(item)) {
                return true;
            }
        }
        return false;
    }

    @lombok.Value
    public static class ValidationResult {
        boolean valid;
        List<String> currentVersions;
        List<String> proposedVersions;
        List<String> errors;
    }
}
