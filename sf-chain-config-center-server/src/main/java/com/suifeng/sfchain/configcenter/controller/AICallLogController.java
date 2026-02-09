package com.suifeng.sfchain.configcenter.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Compatibility endpoints for legacy UI log pages.
 */
@RestController
@RequestMapping("${sf-chain.path.api-prefix:/sf-chain}/ai-logs")
public class AICallLogController {

    private final JdbcTemplate jdbcTemplate;

    public AICallLogController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getAllLogSummaries() {
        String sql = "SELECT id, trace_id, operation_type, model_name, status, latency_ms, created_at " +
                "FROM sfchain_cp_call_logs ORDER BY created_at DESC LIMIT 500";
        return safeQuery(sql);
    }

    @GetMapping("/{callId}")
    public Map<String, Object> getFullLog(@PathVariable String callId) {
        String sqlByTrace = "SELECT id, trace_id, operation_type, model_name, status, latency_ms, created_at " +
                "FROM sfchain_cp_call_logs WHERE trace_id = ? ORDER BY created_at DESC LIMIT 1";
        List<Map<String, Object>> byTrace = safeQuery(sqlByTrace, callId);
        if (!byTrace.isEmpty()) {
            return withFullFields(byTrace.get(0));
        }

        try {
            long id = Long.parseLong(callId);
            String sqlById = "SELECT id, trace_id, operation_type, model_name, status, latency_ms, created_at " +
                    "FROM sfchain_cp_call_logs WHERE id = ? ORDER BY created_at DESC LIMIT 1";
            List<Map<String, Object>> byId = safeQuery(sqlById, id);
            if (!byId.isEmpty()) {
                return withFullFields(byId.get(0));
            }
        } catch (NumberFormatException ignored) {
            // not numeric id, ignore
        }

        return withFullFields(defaultSummary("NOT_FOUND"));
    }

    @GetMapping("/operation/{operationType}")
    public List<Map<String, Object>> getByOperation(@PathVariable String operationType) {
        String sql = "SELECT id, trace_id, operation_type, model_name, status, latency_ms, created_at " +
                "FROM sfchain_cp_call_logs WHERE operation_type = ? ORDER BY created_at DESC LIMIT 500";
        return safeQuery(sql, operationType);
    }

    @GetMapping("/model/{modelName}")
    public List<Map<String, Object>> getByModel(@PathVariable String modelName) {
        String sql = "SELECT id, trace_id, operation_type, model_name, status, latency_ms, created_at " +
                "FROM sfchain_cp_call_logs WHERE model_name = ? ORDER BY created_at DESC LIMIT 500";
        return safeQuery(sql, modelName);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        List<Map<String, Object>> summaries = getAllLogSummaries();
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
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            int affected = jdbcTemplate.update("DELETE FROM sfchain_cp_call_logs");
            response.put("success", true);
            response.put("message", "logs cleared");
            response.put("data", Map.of("deleted", affected));
            return response;
        } catch (DataAccessException ex) {
            response.put("success", true);
            response.put("message", "logs table unavailable, nothing to clear");
            response.put("data", Map.of("deleted", 0));
            return response;
        }
    }

    private List<Map<String, Object>> safeQuery(String sql, Object... args) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                String traceId = rs.getString("trace_id");
                long id = rs.getLong("id");
                item.put("callId", traceId == null || traceId.isBlank() ? String.valueOf(id) : traceId);
                item.put("operationType", defaultString(rs.getString("operation_type"), "UNKNOWN"));
                item.put("modelName", defaultString(rs.getString("model_name"), "UNKNOWN"));
                item.put("callTime", toDateTime(rs.getTimestamp("created_at")));
                item.put("duration", rs.getLong("latency_ms"));
                item.put("status", defaultString(rs.getString("status"), "SUCCESS"));
                item.put("errorMessage", null);
                item.put("frequency", 1);
                item.put("lastAccessTime", toDateTime(rs.getTimestamp("created_at")));
                return item;
            }, args);
        } catch (DataAccessException ex) {
            return new ArrayList<>();
        }
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

    private static LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
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
}

