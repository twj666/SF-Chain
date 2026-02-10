package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接入日志索引后台维护任务
 */
@Slf4j
public class IngestionIndexMaintenanceService {

    private final SfChainIngestionProperties properties;
    private final AICallLogIngestionStore ingestionStore;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong runCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong rebuiltFileCount = new AtomicLong();
    private final AtomicLong totalDurationNanos = new AtomicLong();

    public IngestionIndexMaintenanceService(
            SfChainIngestionProperties properties,
            AICallLogIngestionStore ingestionStore) {
        this.properties = properties;
        this.ingestionStore = ingestionStore;
    }

    @PostConstruct
    public void start() {
        long intervalSeconds = Math.max(properties.getIndexMaintenanceIntervalSeconds(), 30);
        scheduler.scheduleWithFixedDelay(this::rebuildOnce, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    public int rebuildOnce() {
        long start = System.nanoTime();
        runCount.incrementAndGet();
        try {
            int count = ingestionStore.rebuildIndexes();
            totalDurationNanos.addAndGet(System.nanoTime() - start);
            successCount.incrementAndGet();
            rebuiltFileCount.addAndGet(count);
            if (count > 0) {
                log.debug("后台索引维护完成, rebuilt={}", count);
            }
            return count;
        } catch (Exception e) {
            totalDurationNanos.addAndGet(System.nanoTime() - start);
            failureCount.incrementAndGet();
            log.debug("后台索引维护异常: {}", e.getMessage());
            return 0;
        }
    }

    public IngestionIndexMaintenanceMetrics metrics() {
        long runs = runCount.get();
        double avgMs = runs == 0 ? 0.0 : totalDurationNanos.get() / 1_000_000.0 / runs;
        return IngestionIndexMaintenanceMetrics.builder()
                .runCount(runs)
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .rebuiltFileCount(rebuiltFileCount.get())
                .averageDurationMs(avgMs)
                .build();
    }
}
