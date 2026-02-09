package com.suifeng.sfchain.core.logging.ingestion;

import lombok.Value;

import java.util.List;

/**
 * 接入日志游标分页结果
 */
@Value
public class AICallLogIngestionPage {
    List<AICallLogIngestionRecord> records;
    Integer nextCursor;
    boolean hasMore;
}
