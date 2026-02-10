package com.suifeng.sfchain.core.logging.upload;

import lombok.Builder;
import lombok.Value;

/**
 * 日志上报统计快照
 */
@Value
@Builder
public class AICallLogUploadStats {
    int queueSize;
    long sampledOutCount;
    long droppedCount;
    long successCount;
    long failedCount;

    public static AICallLogUploadStats empty() {
        return AICallLogUploadStats.builder().build();
    }
}
