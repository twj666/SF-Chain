package com.suifeng.sfchain.configcenter.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.configcenter.dto.ApiKeyDtos;
import com.suifeng.sfchain.configcenter.logging.AICallLogRouteContext;
import com.suifeng.sfchain.configcenter.service.ControlPlaneService;
import com.suifeng.sfchain.core.logging.AICallLog;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionPage;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionRecord;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/logs/ai-calls")
@ConditionalOnProperty(prefix = "sf-chain.ingestion", name = "enabled", havingValue = "true")
public class AICallLogIngestionController {

    private final AICallLogManager logManager;
    private final SfChainIngestionProperties ingestionProperties;
    private final MinuteWindowQuotaService quotaService;
    private final AICallLogIngestionStore ingestionStore;
    private final IngestionContractHealthTracker contractHealthTracker;
    private final ControlPlaneService controlPlaneService;

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey,
            @RequestHeader(value = "X-SF-CONTRACT-VERSION", required = false) String headerContractVersion,
            @RequestBody AICallLogUploadBatchRequest request) {
        if (!isApiKeyValid(apiKey)) {
            String tenantId = request == null ? null : request.getTenantId();
            String appId = request == null ? null : request.getAppId();
            if (!isTenantApiKeyValid(apiKey, tenantId, appId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
            }
        }
        String contractVersion = resolveContractVersion(request, headerContractVersion);
        if (!isContractVersionSupported(contractVersion)) {
            contractHealthTracker.recordContractRejected();
            return ResponseEntity.badRequest().body(Map.of("message", "unsupported contract version"));
        }

        List<AICallLogUploadItem> items = request == null ? Collections.emptyList() : request.getItems();
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "items must not be empty"));
        }
        if (ingestionProperties.isRequireTenantApp()
                && (isBlank(request.getTenantId()) || isBlank(request.getAppId()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "tenantId and appId are required"));
        }
        if (items.size() > Math.max(ingestionProperties.getMaxBatchSize(), 1)) {
            return ResponseEntity.badRequest().body(Map.of("message", "batch too large"));
        }
        if (!quotaService.tryAcquire(request.getTenantId(), request.getAppId(), items.size())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "quota exceeded"));
        }

        try (AICallLogRouteContext.Scope ignored =
                     AICallLogRouteContext.use(request.getTenantId(), request.getAppId())) {
            for (AICallLogUploadItem item : items) {
                logManager.addLog(toAICallLog(item));
            }
        }
        contractHealthTracker.recordAccepted();

        return ResponseEntity.ok(Map.of(
                "accepted", items.size(),
                "contractVersion", contractVersion,
                "tenantId", request.getTenantId(),
                "appId", request.getAppId()
        ));
    }

    @GetMapping("/records")
    public ResponseEntity<?> queryRecords(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey,
            @RequestParam String tenantId,
            @RequestParam String appId,
            @RequestParam(defaultValue = "100") int limit) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        int safeLimit = Math.min(Math.max(limit, 1), Math.max(ingestionProperties.getMaxQueryLimit(), 1));
        List<AICallLogIngestionRecord> records = ingestionStore.query(tenantId, appId, safeLimit);
        return ResponseEntity.ok(Map.of("tenantId", tenantId, "appId", appId, "count", records.size(), "records", records));
    }

    @GetMapping("/records/page")
    public ResponseEntity<?> queryRecordsByCursor(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey,
            @RequestParam String tenantId,
            @RequestParam String appId,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "100") int limit) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        int safeLimit = Math.min(Math.max(limit, 1), Math.max(ingestionProperties.getMaxQueryLimit(), 1));
        int safeCursor = Math.max(cursor, 0);
        AICallLogIngestionPage page = ingestionStore.queryPage(tenantId, appId, safeCursor, safeLimit);
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId,
                "appId", appId,
                "cursor", safeCursor,
                "limit", safeLimit,
                "nextCursor", page.getNextCursor(),
                "hasMore", page.isHasMore(),
                "count", page.getRecords().size(),
                "records", page.getRecords()
        ));
    }

    @DeleteMapping("/records/expired")
    public ResponseEntity<?> purgeExpired(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid api key"));
        }
        int deleted = ingestionStore.purgeExpired();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    private boolean isApiKeyValid(String apiKey) {
        String expected = ingestionProperties.getApiKey();
        return expected != null && !expected.isBlank() && expected.equals(apiKey);
    }

    private boolean isTenantApiKeyValid(String apiKey, String tenantId, String appId) {
        if (isBlank(apiKey) || isBlank(tenantId) || isBlank(appId)) {
            return false;
        }
        ApiKeyDtos.ValidateApiKeyRequest request = new ApiKeyDtos.ValidateApiKeyRequest();
        request.setApiKey(apiKey);
        request.setTenantId(tenantId);
        request.setAppId(appId);
        ApiKeyDtos.ValidateApiKeyResponse response = controlPlaneService.validateApiKey(request);
        return response != null && response.isValid();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveContractVersion(AICallLogUploadBatchRequest request, String headerContractVersion) {
        String fromBody = request == null ? null : request.getContractVersion();
        if (!isBlank(fromBody)) {
            return fromBody.trim();
        }
        if (!isBlank(headerContractVersion)) {
            return headerContractVersion.trim();
        }
        return "v1";
    }

    private boolean isContractVersionSupported(String version) {
        Set<String> supported = resolveSupportedVersions();
        return supported.contains(version);
    }

    private Set<String> resolveSupportedVersions() {
        Set<String> versions = new HashSet<>();
        String single = ingestionProperties.getSupportedContractVersion();
        if (!isBlank(single)) {
            versions.add(single.trim());
        }
        List<String> multiple = ingestionProperties.getSupportedContractVersions();
        if (multiple != null) {
            for (String item : multiple) {
                if (!isBlank(item)) {
                    versions.add(item.trim());
                }
            }
        }
        if (versions.isEmpty()) {
            versions.add("v1");
        }
        return versions;
    }

    private static AICallLog toAICallLog(AICallLogUploadItem item) {
        AICallLog.AIRequestParams params = AICallLog.AIRequestParams.builder()
                .maxTokens(item.getMaxTokens())
                .temperature(item.getTemperature())
                .jsonOutput(item.getJsonOutput())
                .thinking(item.getThinking())
                .build();

        AICallLog.CallStatus status;
        try {
            status = item.getStatus() == null ? AICallLog.CallStatus.SUCCESS : AICallLog.CallStatus.valueOf(item.getStatus());
        } catch (IllegalArgumentException ex) {
            status = AICallLog.CallStatus.FAILED;
        }

        LocalDateTime now = LocalDateTime.now();
        return AICallLog.builder()
                .callId(item.getCallId())
                .operationType(item.getOperationType())
                .modelName(item.getModelName())
                .callTime(item.getCallTime() == null ? now : item.getCallTime())
                .duration(item.getDuration())
                .status(status)
                .input(item.getInput())
                .prompt(item.getPrompt())
                .requestParams(params)
                .rawResponse(item.getRawResponse())
                .output(item.getOutput())
                .errorMessage(item.getErrorMessage())
                .frequency(1)
                .lastAccessTime(now)
                .build();
    }

    @Data
    public static class AICallLogUploadBatchRequest {
        private String contractVersion;
        private String tenantId;
        private String appId;
        private List<AICallLogUploadItem> items;
    }
}
