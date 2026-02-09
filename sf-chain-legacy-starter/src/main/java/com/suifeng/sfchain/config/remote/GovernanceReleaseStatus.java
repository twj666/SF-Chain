package com.suifeng.sfchain.config.remote;

/**
 * 治理发布状态机
 */
public enum GovernanceReleaseStatus {
    NOOP,
    SKIPPED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    ROLLED_BACK,
    RETRY_WAIT,
    COOLDOWN
}
