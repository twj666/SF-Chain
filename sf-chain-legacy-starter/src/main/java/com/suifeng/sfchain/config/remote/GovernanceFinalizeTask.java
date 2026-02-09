package com.suifeng.sfchain.config.remote;

import lombok.Data;

/**
 * 待确认的治理终态回调任务
 */
@Data
public class GovernanceFinalizeTask {

    private String snapshotVersion;

    private GovernanceSyncApplyResult result;
}
