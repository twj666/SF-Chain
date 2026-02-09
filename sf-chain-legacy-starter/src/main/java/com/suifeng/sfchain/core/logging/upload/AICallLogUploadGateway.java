package com.suifeng.sfchain.core.logging.upload;

import com.suifeng.sfchain.core.logging.AICallLog;

/**
 * AI调用日志上报网关
 */
public interface AICallLogUploadGateway {

    void publish(AICallLog callLog);

    default AICallLogUploadStats stats() {
        return AICallLogUploadStats.empty();
    }

    AICallLogUploadGateway NO_OP = callLog -> {
    };
}
