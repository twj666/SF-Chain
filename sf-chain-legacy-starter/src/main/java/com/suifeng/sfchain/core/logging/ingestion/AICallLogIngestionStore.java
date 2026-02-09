package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;

import java.util.List;

/**
 * 接入日志持久化存储
 */
public interface AICallLogIngestionStore {

    void saveBatch(String tenantId, String appId, List<AICallLogUploadItem> items);

    AICallLogIngestionStore NO_OP = (tenantId, appId, items) -> {
    };
}
