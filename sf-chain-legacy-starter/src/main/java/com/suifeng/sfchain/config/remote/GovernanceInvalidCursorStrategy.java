package com.suifeng.sfchain.config.remote;

/**
 * finalize 对账无效游标处理策略
 */
public enum GovernanceInvalidCursorStrategy {
    /**
     * 重置游标并尝试继续对账
     */
    RESET_AND_RETRY,
    /**
     * 快速失败并等待下一轮同步
     */
    FAIL_FAST
}
