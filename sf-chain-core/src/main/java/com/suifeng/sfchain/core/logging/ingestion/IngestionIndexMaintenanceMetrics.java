package com.suifeng.sfchain.core.logging.ingestion;

import lombok.Builder;
import lombok.Value;

/**
 * 索引维护指标快照
 */
@Value
@Builder
public class IngestionIndexMaintenanceMetrics {
    long runCount;
    long successCount;
    long failureCount;
    long rebuiltFileCount;
    double averageDurationMs;
}
