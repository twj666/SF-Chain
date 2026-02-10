package com.suifeng.sfchain.config.remote;

/**
 * 治理同步应用器抽象
 */
public interface GovernanceSyncApplier {

    GovernanceSyncApplyResult apply(RemoteIngestionGovernanceSnapshot snapshot);

    ApplierState snapshotState();

    void restoreState(ApplierState state);

    @lombok.Data
    class ApplierState {
        private String failedReleaseId;
        private long retryBlockedUntilEpochMs;
        private long rollbackCooldownUntilEpochMs;
        private String activeReleaseId;
        private int activeReleasePriority;
    }
}
