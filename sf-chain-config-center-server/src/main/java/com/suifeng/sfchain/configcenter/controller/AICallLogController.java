package com.suifeng.sfchain.configcenter.controller;

import com.suifeng.sfchain.configcenter.entity.AppEntity;
import com.suifeng.sfchain.configcenter.entity.TenantEntity;
import com.suifeng.sfchain.configcenter.repository.AppRepository;
import com.suifeng.sfchain.configcenter.repository.TenantRepository;
import com.suifeng.sfchain.core.logging.AICallLog;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.AICallLogSummary;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionRecord;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compatibility endpoints for legacy UI log pages.
 */
@RestController
@RequestMapping("${sf-chain.path.api-prefix:/sf-chain}/ai-logs")
public class AICallLogController {

    private static final String CONFIG_CENTER_TENANT_ID = "__config_center__";
    private static final String CONFIG_CENTER_APP_ID = "__config_center__";
    private static final int DEFAULT_LIMIT = 500;

    private final AICallLogManager logManager;
    private final AICallLogIngestionStore ingestionStore;
    private final TenantRepository tenantRepository;
    private final AppRepository appRepository;

    public AICallLogController(
            ObjectProvider<AICallLogManager> logManagerProvider,
            ObjectProvider<AICallLogIngestionStore> ingestionStoreProvider,
            TenantRepository tenantRepository,
            AppRepository appRepository) {
        this.logManager = logManagerProvider.getIfAvailable();
        this.ingestionStore = ingestionStoreProvider.getIfAvailable();
        this.tenantRepository = tenantRepository;
        this.appRepository = appRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getAllLogSummaries() {
        return getAllLogSummaries(null, null);
    }

    @GetMapping(params = {"tenantId", "appId"})
    public List<Map<String, Object>> getAllLogSummariesByScope(
            @RequestParam String tenantId,
            @RequestParam String appId) {
        return getAllLogSummaries(tenantId, appId);
    }

    private List<Map<String, Object>> getAllLogSummaries(String tenantId, String appId) {
        if (hasScope(tenantId, appId)) {
            return loadPersistedSummaries(DEFAULT_LIMIT, tenantId, appId);
        }
        if (logManager != null) {
            List<Map<String, Object>> inMemory = logManager.getAllLogSummaries().stream()
                    .map(this::summaryToMap)
                    .collect(Collectors.toList());
            if (!inMemory.isEmpty()) {
                return inMemory;
            }
        }
        return loadPersistedSummaries(DEFAULT_LIMIT, null, null);
    }

    @GetMapping("/{callId}")
    public Map<String, Object> getFullLog(
            @PathVariable String callId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {
        if (!hasScope(tenantId, appId) && logManager != null) {
            AICallLog log = logManager.getFullLog(callId);
            if (log != null) {
                return fullLogToMap(log);
            }
        }
        for (AICallLogIngestionRecord record : loadPersistedRecords(2000, tenantId, appId)) {
            if (record == null || record.getItem() == null) {
                continue;
            }
            if (Objects.equals(defaultString(record.getItem().getCallId(), "UNKNOWN"), callId)) {
                return recordToFullMap(record);
            }
        }
        return withFullFields(defaultSummary("NOT_FOUND"));
    }

    @GetMapping("/operation/{operationType}")
    public List<Map<String, Object>> getByOperation(
            @PathVariable String operationType,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {
        if (!hasScope(tenantId, appId) && logManager != null) {
            List<Map<String, Object>> inMemory = logManager.getLogSummariesByOperation(operationType).stream()
                    .map(this::summaryToMap)
                    .collect(Collectors.toList());
            if (!inMemory.isEmpty()) {
                return inMemory;
            }
        }
        return loadPersistedSummaries(2000, tenantId, appId).stream()
                .filter(it -> Objects.equals(String.valueOf(it.get("operationType")), operationType))
                .collect(Collectors.toList());
    }

    @GetMapping("/model/{modelName}")
    public List<Map<String, Object>> getByModel(
            @PathVariable String modelName,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {
        if (!hasScope(tenantId, appId) && logManager != null) {
            List<Map<String, Object>> inMemory = logManager.getLogSummariesByModel(modelName).stream()
                    .map(this::summaryToMap)
                    .collect(Collectors.toList());
            if (!inMemory.isEmpty()) {
                return inMemory;
            }
        }
        return loadPersistedSummaries(2000, tenantId, appId).stream()
                .filter(it -> Objects.equals(String.valueOf(it.get("modelName")), modelName))
                .collect(Collectors.toList());
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {
        if (!hasScope(tenantId, appId) && logManager != null) {
            AICallLogManager.LogStatistics stats = logManager.getStatistics();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalCalls", stats.getTotalCalls());
            result.put("successfulCalls", stats.getSuccessCalls());
            result.put("failedCalls", Math.max(0L, stats.getTotalCalls() - stats.getSuccessCalls()));
            result.put("totalTokensUsed", 0);
            result.put("averageResponseTime", Math.round(stats.getAverageDuration()));
            result.put("callsByOperation", stats.getOperationCounts() == null ? Map.of() : stats.getOperationCounts());
            result.put("callsByModel", stats.getModelCounts() == null ? Map.of() : stats.getModelCounts());
            return result;
        }

        List<Map<String, Object>> summaries = getAllLogSummaries(tenantId, appId);
        int totalCalls = summaries.size();
        int successfulCalls = (int) summaries.stream()
                .filter(it -> "SUCCESS".equalsIgnoreCase(String.valueOf(it.get("status"))))
                .count();
        int failedCalls = totalCalls - successfulCalls;
        long averageResponseTime = totalCalls == 0 ? 0L : Math.round(
                summaries.stream()
                        .mapToLong(it -> toLong(it.get("duration")))
                        .average()
                        .orElse(0D)
        );

        Map<String, Long> callsByOperation = summaries.stream()
                .collect(Collectors.groupingBy(
                        it -> String.valueOf(it.get("operationType")),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> callsByModel = summaries.stream()
                .collect(Collectors.groupingBy(
                        it -> String.valueOf(it.get("modelName")),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", totalCalls);
        result.put("successfulCalls", successfulCalls);
        result.put("failedCalls", failedCalls);
        result.put("totalTokensUsed", 0);
        result.put("averageResponseTime", averageResponseTime);
        result.put("callsByOperation", callsByOperation);
        result.put("callsByModel", callsByModel);
        return result;
    }

    @DeleteMapping
    public Map<String, Object> clearLogs() {
        if (logManager != null) {
            logManager.clearLogs();
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "in-memory logs cleared");
        response.put("data", Map.of("deleted", 0));
        return response;
    }

    private static Map<String, Object> withFullFields(Map<String, Object> summary) {
        Map<String, Object> full = new LinkedHashMap<>(summary);
        full.put("input", null);
        full.put("prompt", null);
        full.put("rawResponse", null);
        full.put("output", null);
        return full;
    }

    private static Map<String, Object> defaultSummary(String callId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("callId", callId);
        item.put("operationType", "UNKNOWN");
        item.put("modelName", "UNKNOWN");
        item.put("callTime", LocalDateTime.now());
        item.put("duration", 0);
        item.put("status", "FAILED");
        item.put("errorMessage", "log not found");
        item.put("frequency", 1);
        item.put("lastAccessTime", LocalDateTime.now());
        return item;
    }

    private static String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Map<String, Object> summaryToMap(AICallLogSummary summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("callId", summary.getCallId());
        item.put("operationType", defaultString(summary.getOperationType(), "UNKNOWN"));
        item.put("modelName", defaultString(summary.getModelName(), "UNKNOWN"));
        item.put("callTime", summary.getCallTime() == null ? LocalDateTime.now() : summary.getCallTime());
        item.put("duration", summary.getDuration());
        item.put("status", summary.getStatus() == null ? "FAILED" : summary.getStatus().name());
        item.put("errorMessage", summary.getErrorMessage());
        item.put("frequency", summary.getFrequency());
        item.put("lastAccessTime", summary.getLastAccessTime() == null ? LocalDateTime.now() : summary.getLastAccessTime());
        if (summary.getRequestParams() != null) {
            item.put("requestParams", summary.getRequestParams());
        }
        return item;
    }

    private Map<String, Object> fullLogToMap(AICallLog log) {
        Map<String, Object> item = summaryToMap(AICallLogSummary.fromFullLog(log));
        item.put("input", log.getInput());
        item.put("prompt", log.getPrompt());
        item.put("rawResponse", log.getRawResponse());
        item.put("output", log.getOutput());
        return item;
    }

    private List<Map<String, Object>> loadPersistedSummaries(int limit, String tenantId, String appId) {
        return loadPersistedRecords(limit, tenantId, appId).stream()
                .map(this::recordToSummaryMap)
                .collect(Collectors.toList());
    }

    private List<AICallLogIngestionRecord> loadPersistedRecords(int limit, String tenantId, String appId) {
        if (ingestionStore == null) {
            return List.of();
        }
        List<AICallLogIngestionRecord> records = new ArrayList<>();
        if (hasScope(tenantId, appId)) {
            records.addAll(ingestionStore.query(tenantId, appId, limit));
        } else {
            Set<String> appKeys = new LinkedHashSet<>();
            appKeys.add(CONFIG_CENTER_TENANT_ID + ":" + CONFIG_CENTER_APP_ID);

            Set<String> activeTenants = tenantRepository.findAll().stream()
                    .filter(TenantEntity::isActive)
                    .map(TenantEntity::getTenantId)
                    .collect(Collectors.toSet());
            for (AppEntity app : appRepository.findAll()) {
                if (!app.isActive() || !activeTenants.contains(app.getTenantId())) {
                    continue;
                }
                appKeys.add(app.getTenantId() + ":" + app.getAppId());
            }

            int perAppLimit = Math.max(20, limit / Math.max(appKeys.size(), 1));
            for (String key : appKeys) {
                String[] parts = key.split(":", 2);
                if (parts.length != 2) {
                    continue;
                }
                records.addAll(ingestionStore.query(parts[0], parts[1], perAppLimit));
            }
        }
        return records.stream()
                .sorted(Comparator.comparing(AICallLogIngestionRecord::getIngestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Map<String, Object> recordToSummaryMap(AICallLogIngestionRecord record) {
        Map<String, Object> full = recordToFullMap(record);
        full.remove("input");
        full.remove("prompt");
        full.remove("rawResponse");
        full.remove("output");
        return full;
    }

    private Map<String, Object> recordToFullMap(AICallLogIngestionRecord record) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (record == null || record.getItem() == null) {
            item.put("callId", "UNKNOWN");
            item.put("operationType", "UNKNOWN");
            item.put("modelName", "UNKNOWN");
            item.put("callTime", LocalDateTime.now());
            item.put("duration", 0L);
            item.put("status", "FAILED");
            item.put("errorMessage", "invalid ingestion record");
            item.put("frequency", 1);
            item.put("lastAccessTime", LocalDateTime.now());
            item.put("tenantId", null);
            item.put("appId", null);
            item.put("input", null);
            item.put("prompt", null);
            item.put("rawResponse", null);
            item.put("output", null);
            return item;
        }
        item.put("tenantId", record.getTenantId());
        item.put("appId", record.getAppId());
        item.put("callId", defaultString(record.getItem().getCallId(), "UNKNOWN"));
        item.put("operationType", defaultString(record.getItem().getOperationType(), "UNKNOWN"));
        item.put("modelName", defaultString(record.getItem().getModelName(), "UNKNOWN"));
        item.put("callTime", record.getItem().getCallTime() == null ? record.getIngestedAt() : record.getItem().getCallTime());
        item.put("duration", record.getItem().getDuration());
        item.put("status", defaultString(record.getItem().getStatus(), "FAILED"));
        item.put("errorMessage", record.getItem().getErrorMessage());
        item.put("frequency", 1);
        item.put("lastAccessTime", record.getIngestedAt() == null ? LocalDateTime.now() : record.getIngestedAt());
        item.put("input", record.getItem().getInput());
        item.put("prompt", record.getItem().getPrompt());
        item.put("rawResponse", record.getItem().getRawResponse());
        item.put("output", record.getItem().getOutput());
        return item;
    }

    private static boolean hasScope(String tenantId, String appId) {
        return tenantId != null && !tenantId.isBlank() && appId != null && !appId.isBlank();
    }
}
