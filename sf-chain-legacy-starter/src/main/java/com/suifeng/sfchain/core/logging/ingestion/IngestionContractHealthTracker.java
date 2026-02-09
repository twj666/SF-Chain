package com.suifeng.sfchain.core.logging.ingestion;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 接入契约健康统计
 */
public class IngestionContractHealthTracker {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong contractRejectedRequests = new AtomicLong();

    public void recordAccepted() {
        totalRequests.incrementAndGet();
    }

    public void recordContractRejected() {
        totalRequests.incrementAndGet();
        contractRejectedRequests.incrementAndGet();
    }

    public IngestionContractHealthSnapshot snapshot() {
        long total = totalRequests.get();
        long rejected = contractRejectedRequests.get();
        double rejectRate = total <= 0 ? 0D : (double) rejected / (double) total;
        return new IngestionContractHealthSnapshot(total, rejected, rejectRate);
    }
}
