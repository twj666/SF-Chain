package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 接入日志索引后台维护任务
 */
@Slf4j
public class IngestionIndexMaintenanceService {

    private final SfChainIngestionProperties properties;
    private final AICallLogIngestionStore ingestionStore;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public IngestionIndexMaintenanceService(
            SfChainIngestionProperties properties,
            AICallLogIngestionStore ingestionStore) {
        this.properties = properties;
        this.ingestionStore = ingestionStore;
    }

    @PostConstruct
    public void start() {
        long intervalSeconds = Math.max(properties.getIndexMaintenanceIntervalSeconds(), 30);
        scheduler.scheduleWithFixedDelay(this::rebuildSafely, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private void rebuildSafely() {
        try {
            int count = ingestionStore.rebuildIndexes();
            if (count > 0) {
                log.debug("后台索引维护完成, rebuilt={}", count);
            }
        } catch (Exception e) {
            log.debug("后台索引维护异常: {}", e.getMessage());
        }
    }
}
