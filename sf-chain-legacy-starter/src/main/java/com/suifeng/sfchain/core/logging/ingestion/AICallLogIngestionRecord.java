package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 接入日志记录
 */
@Value
public class AICallLogIngestionRecord {
    String tenantId;
    String appId;
    LocalDateTime ingestedAt;
    AICallLogUploadItem item;
}
