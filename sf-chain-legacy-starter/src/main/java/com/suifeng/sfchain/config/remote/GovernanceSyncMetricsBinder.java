package com.suifeng.sfchain.config.remote;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;

/**
 * 治理同步指标 Micrometer 绑定器
 */
@RequiredArgsConstructor
public class GovernanceSyncMetricsBinder implements MeterBinder {

    private final RemoteConfigSyncService syncService;

    @Override
    public void bindTo(MeterRegistry registry) {
        registerGauge(registry, "sfchain.governance.sync.run.count",
                metrics -> metrics.getSyncRunCount());
        registerGauge(registry, "sfchain.governance.sync.failure.count",
                metrics -> metrics.getSyncFailureCount());
        registerGauge(registry, "sfchain.governance.lease.acquire.attempt.count",
                metrics -> metrics.getLeaseAcquireAttempts());
        registerGauge(registry, "sfchain.governance.lease.acquire.success.count",
                metrics -> metrics.getLeaseAcquireSuccess());
        registerGauge(registry, "sfchain.governance.lease.acquire.remote.success.count",
                metrics -> metrics.getLeaseAcquireRemoteSuccess());
        registerGauge(registry, "sfchain.governance.lease.acquire.local.success.count",
                metrics -> metrics.getLeaseAcquireLocalSuccess());
        registerGauge(registry, "sfchain.governance.finalize.reconcile.attempt.count",
                metrics -> metrics.getFinalizeReconcileAttempts());
        registerGauge(registry, "sfchain.governance.finalize.reconcile.success.count",
                metrics -> metrics.getFinalizeReconcileSuccess());
        registerGauge(registry, "sfchain.governance.finalize.reconcile.failure.count",
                metrics -> metrics.getFinalizeReconcileFailure());
        registerGauge(registry, "sfchain.governance.finalize.reconcile.invalid_cursor.count",
                metrics -> metrics.getFinalizeReconcileInvalidCursorCount());
        registerGauge(registry, "sfchain.governance.finalize.reconcile.cursor_reset.count",
                metrics -> metrics.getFinalizeReconcileCursorResetCount());
        registerGauge(registry, "sfchain.governance.finalize.retry.attempt.count",
                metrics -> metrics.getFinalizeRetryAttempts());
        registerGauge(registry, "sfchain.governance.finalize.retry.success.count",
                metrics -> metrics.getFinalizeRetrySuccess());
        registerGauge(registry, "sfchain.governance.finalize.retry.failure.count",
                metrics -> metrics.getFinalizeRetryFailure());
    }

    private void registerGauge(
            MeterRegistry registry,
            String name,
            java.util.function.ToDoubleFunction<GovernanceSyncMetricsSnapshot> extractor) {
        Gauge.builder(name, syncService, service -> extractor.applyAsDouble(service.metrics()))
                .register(registry);
    }
}
