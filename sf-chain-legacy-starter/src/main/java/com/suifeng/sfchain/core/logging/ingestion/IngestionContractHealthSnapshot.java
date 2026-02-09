package com.suifeng.sfchain.core.logging.ingestion;

import lombok.Value;

/**
 * 接入契约健康快照
 */
@Value
public class IngestionContractHealthSnapshot {
    long totalRequests;
    long contractRejectedRequests;
    double rejectRate;
}
