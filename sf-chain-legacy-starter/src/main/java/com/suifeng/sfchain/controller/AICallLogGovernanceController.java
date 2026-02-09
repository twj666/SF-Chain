package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionIndexMaintenanceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置中心日志治理接口
 */
@RestController
@RequestMapping("/v1/logs/ai-calls")
@RequiredArgsConstructor
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
