package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;

import java.util.Collections;
import java.util.List;

/**
 * 接入日志持久化存储
 */
public interface AICallLogIngestionStore {

    void saveBatch(String tenantId, String appId, List<AICallLogUploadItem> items);

    default List<AICallLogIngestionRecord> query(String tenantId, String appId, int limit) {
        return Collections.emptyList();
    }

    default int purgeExpired() {
        return 0;
    }

    AICallLogIngestionStore NO_OP = (tenantId, appId, items) -> {
    };
}
