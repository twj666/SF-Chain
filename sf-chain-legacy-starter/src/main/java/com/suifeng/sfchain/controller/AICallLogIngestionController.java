package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.AICallLog;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionRecord;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 配置中心AI调用日志接入接口
 */
@RestController
@RequestMapping("/v1/logs/ai-calls")
@RequiredArgsConstructor
public class AICallLogIngestionController {

    private final AICallLogManager logManager;
    private final SfChainIngestionProperties ingestionProperties;
    private final MinuteWindowQuotaService quotaService;
    private final AICallLogIngestionStore ingestionStore;

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(
            @RequestHeader(value = "X-SF-API-KEY", required = false) String apiKey,
            @RequestBody AICallLogUploadBatchRequest request) {
        if (!isApiKeyValid(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid api key"));
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

        ingestionStore.saveBatch(request.getTenantId(), request.getAppId(), items);

        for (AICallLogUploadItem item : items) {
            logManager.addLog(toAICallLog(item));
        }

        return ResponseEntity.ok(Map.of(
                "accepted", items.size(),
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
        private String tenantId;
        private String appId;
        private List<AICallLogUploadItem> items;
    }
}
