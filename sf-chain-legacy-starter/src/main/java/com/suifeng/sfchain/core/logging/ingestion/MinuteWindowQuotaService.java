package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.config.SfChainIngestionProperties;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 每分钟窗口租户应用配额
 */
public class MinuteWindowQuotaService {

    private final SfChainIngestionProperties properties;
    private final ConcurrentHashMap<String, CounterWindow> windows = new ConcurrentHashMap<>();

    public MinuteWindowQuotaService(SfChainIngestionProperties properties) {
        this.properties = properties;
    }

    public boolean tryAcquire(String tenantId, String appId, int size) {
        int limit = Math.max(properties.getPerTenantAppPerMinuteLimit(), 1);
        long minute = Instant.now().getEpochSecond() / 60;
        String key = (tenantId == null ? "" : tenantId) + "|" + (appId == null ? "" : appId);
        CounterWindow window = windows.compute(key, (k, old) -> {
            if (old == null || old.epochMinute != minute) {
                return new CounterWindow(minute);
            }
            return old;
        });
        while (true) {
            int current = window.count.get();
            int next = current + size;
            if (next > limit) {
                return false;
            }
            if (window.count.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    private static class CounterWindow {
        private final long epochMinute;
        private final AtomicInteger count = new AtomicInteger(0);

        private CounterWindow(long epochMinute) {
            this.epochMinute = epochMinute;
        }
    }
}
