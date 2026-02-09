package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionIndexMaintenanceServiceTest {

    @Test
    void shouldCollectMetricsAfterRebuild() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        AtomicInteger called = new AtomicInteger();
        AICallLogIngestionStore store = new AICallLogIngestionStore() {
            @Override
            public void saveBatch(String tenantId, String appId, List<com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem> items) {
            }

            @Override
            public int rebuildIndexes() {
                called.incrementAndGet();
                return 3;
            }
        };

        IngestionIndexMaintenanceService service = new IngestionIndexMaintenanceService(properties, store);
        int rebuilt = service.rebuildOnce();
        IngestionIndexMaintenanceMetrics metrics = service.metrics();

        assertThat(rebuilt).isEqualTo(3);
        assertThat(called.get()).isEqualTo(1);
        assertThat(metrics.getRunCount()).isEqualTo(1);
        assertThat(metrics.getSuccessCount()).isEqualTo(1);
        assertThat(metrics.getFailureCount()).isEqualTo(0);
        assertThat(metrics.getRebuiltFileCount()).isEqualTo(3);
    }
}
