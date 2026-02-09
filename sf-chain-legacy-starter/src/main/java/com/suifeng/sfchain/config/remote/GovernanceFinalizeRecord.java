package com.suifeng.sfchain.config.remote;

import lombok.Data;

/**
 * 已完成治理终态记录
 */
@Data
public class GovernanceFinalizeRecord {
    private GovernanceReleaseStatus status;
    private long updatedAtEpochMs;
    private String ackId;
    private Long ackVersion;
}
