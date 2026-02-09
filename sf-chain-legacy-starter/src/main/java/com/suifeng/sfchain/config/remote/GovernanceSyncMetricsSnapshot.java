package com.suifeng.sfchain.config.remote;

import lombok.Builder;
import lombok.Value;

/**
 * 治理同步指标快照
 */
@Value
@Builder
public class GovernanceSyncMetricsSnapshot {
    long syncRunCount;
    long syncFailureCount;
    long leaseAcquireAttempts;
    long leaseAcquireSuccess;
    long leaseAcquireRemoteSuccess;
    long leaseAcquireLocalSuccess;
    long finalizeReconcileAttempts;
    long finalizeReconcileSuccess;
    long finalizeReconcileFailure;
    long finalizeReconcileInvalidCursorCount;
    long finalizeReconcileCursorResetCount;
    long finalizeReconcileInvalidCursorFailFastCount;
    long finalizeRetryAttempts;
    long finalizeRetrySuccess;
    long finalizeRetryFailure;
}
