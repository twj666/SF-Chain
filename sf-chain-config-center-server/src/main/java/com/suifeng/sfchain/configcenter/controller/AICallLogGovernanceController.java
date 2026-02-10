package com.suifeng.sfchain.configcenter.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/logs/ai-calls")
@ConditionalOnProperty(prefix = "sf-chain.ingestion", name = "enabled", havingValue = "true")
public class AICallLogGovernanceController {

    private final SfChainIngestionProperties ingestionProperties;
    private final ContractAllowlistGuardService guardService;
    private final ObjectProvider<IngestionIndexMaintenanceService> indexMaintenanceServiceProvider;

    @GetMapping("/index-maintenance/metrics")
    public ResponseEntity<?> metrics(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        IngestionIndexMaintenanceService service = indexMaintenanceServiceProvider.getIfAvailable();
        if (service == null) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        return ResponseEntity.ok(Map.of("enabled", true, "metrics", service.metrics()));
    }

    @GetMapping("/governance-sync/metrics")
    public ResponseEntity<?> governanceSyncMetrics(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        return ResponseEntity.ok(Map.of("enabled", false));
    }

    @PostMapping("/index-maintenance/rebuild")
    public ResponseEntity<?> rebuild(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        IngestionIndexMaintenanceService service = indexMaintenanceServiceProvider.getIfAvailable();
        if (service == null) {
            return ResponseEntity.ok(Map.of("enabled", false, "rebuilt", 0));
        }
        int rebuilt = service.rebuildOnce();
        return ResponseEntity.ok(Map.of("enabled", true, "rebuilt", rebuilt));
    }

    @PostMapping("/contract-allowlist/validate")
    public ResponseEntity<?> validateAllowlist(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey,
            @RequestBody ValidateRequest request) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        ContractAllowlistGuardService.ValidationResult result = guardService.validate(
                request == null ? null : request.getVersions());
        return ResponseEntity.ok(result);
    }

    private boolean isApiKeyValid(String apiKey) {
        String expected = ingestionProperties.getApiKey();
        return expected != null && !expected.isBlank() && expected.equals(apiKey);
    }

    @Data
    public static class ValidateRequest {
        private List<String> versions;
    }
}
