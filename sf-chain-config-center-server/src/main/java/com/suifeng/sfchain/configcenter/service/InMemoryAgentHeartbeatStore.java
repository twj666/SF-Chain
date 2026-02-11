package com.suifeng.sfchain.configcenter.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryAgentHeartbeatStore {

    private static final int RETENTION_SECONDS = 3600;
    private final ConcurrentMap<String, HeartbeatRecord> records = new ConcurrentHashMap<>();

    public void touch(String tenantId, String appId, String instanceId, String source) {
        String key = composeInstanceKey(tenantId, appId, instanceId);
        LocalDateTime now = LocalDateTime.now();
        records.compute(key, (ignored, existing) -> {
            HeartbeatRecord record = existing == null ? new HeartbeatRecord() : existing;
            record.tenantId = tenantId;
            record.appId = appId;
            record.instanceId = instanceId;
            record.lastSeenAt = now;
            record.source = source;
            return record;
        });
    }

    public Map<String, AppHeartbeatSummary> summarizeByApp(int onlineWindowSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime onlineCutoff = now.minusSeconds(Math.max(onlineWindowSeconds, 10));
        LocalDateTime retentionCutoff = now.minusSeconds(RETENTION_SECONDS);

        Map<String, AppHeartbeatSummary> summaries = new HashMap<>();
        for (Map.Entry<String, HeartbeatRecord> entry : records.entrySet()) {
            HeartbeatRecord record = entry.getValue();
            if (record == null || record.lastSeenAt == null) {
                continue;
            }
            if (record.lastSeenAt.isBefore(retentionCutoff)) {
                records.remove(entry.getKey(), record);
                continue;
            }
            String appKey = composeAppKey(record.tenantId, record.appId);
            AppHeartbeatSummary summary = summaries.computeIfAbsent(appKey, ignored -> new AppHeartbeatSummary());
            if (summary.lastSeenAt == null || record.lastSeenAt.isAfter(summary.lastSeenAt)) {
                summary.lastSeenAt = record.lastSeenAt;
            }
            if (!record.lastSeenAt.isBefore(onlineCutoff)) {
                summary.onlineInstanceCount++;
                summary.onlineInstances.add(new OnlineInstanceHeartbeat(record.instanceId, record.lastSeenAt));
            }
        }
        summaries.values().forEach(summary ->
                summary.onlineInstances.sort(Comparator.comparing(OnlineInstanceHeartbeat::getLastSeenAt).reversed())
        );
        return summaries;
    }

    private static String composeInstanceKey(String tenantId, String appId, String instanceId) {
        return tenantId + "|" + appId + "|" + instanceId;
    }

    private static String composeAppKey(String tenantId, String appId) {
        return tenantId + "|" + appId;
    }

    private static final class HeartbeatRecord {
        private String tenantId;
        private String appId;
        private String instanceId;
        private String source;
        private LocalDateTime lastSeenAt;
    }

    @Getter
    public static final class AppHeartbeatSummary {
        private LocalDateTime lastSeenAt;
        private long onlineInstanceCount;
        private final List<OnlineInstanceHeartbeat> onlineInstances = new ArrayList<>();
    }

    @Getter
    public static final class OnlineInstanceHeartbeat {
        private final String instanceId;
        private final LocalDateTime lastSeenAt;

        private OnlineInstanceHeartbeat(String instanceId, LocalDateTime lastSeenAt) {
            this.instanceId = instanceId;
            this.lastSeenAt = lastSeenAt;
        }
    }
}
