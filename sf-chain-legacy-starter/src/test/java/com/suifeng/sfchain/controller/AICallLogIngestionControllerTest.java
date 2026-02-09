package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionPage;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionRecord;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AICallLogIngestionControllerTest {

    @Test
    void shouldRejectRequestWhenApiKeyInvalid() {
        AICallLogIngestionController controller = newController("center-key");
        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("wrong-key", null, buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldIngestLogsWhenApiKeyValid() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        AICallLogManager manager = new AICallLogManager(logProps);
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        AICallLogIngestionController controller = new AICallLogIngestionController(
                manager,
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                AICallLogIngestionStore.NO_OP
        );

        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("center-key", null, buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("accepted", 1);
        assertThat(manager.getFullLog("call-100")).isNotNull();
    }

    private static AICallLogIngestionController newController(String apiKey) {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey(apiKey);
        return new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                AICallLogIngestionStore.NO_OP
        );
    }

    private static AICallLogIngestionController.AICallLogUploadBatchRequest buildRequest() {
        AICallLogUploadItem item = AICallLogUploadItem.builder()
                .callId("call-100")
                .operationType("TEST_OPERATION")
                .modelName("test-model")
                .callTime(LocalDateTime.now())
                .duration(12L)
                .status("SUCCESS")
                .build();
        AICallLogIngestionController.AICallLogUploadBatchRequest request =
                new AICallLogIngestionController.AICallLogUploadBatchRequest();
        request.setTenantId("t-1");
        request.setAppId("a-1");
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void shouldRejectWhenTenantAppRequiredButMissing() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        ingestionProps.setRequireTenantApp(true);

        AICallLogIngestionController controller = new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                AICallLogIngestionStore.NO_OP
        );
        AICallLogIngestionController.AICallLogUploadBatchRequest request = buildRequest();
        request.setTenantId(null);

        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("center-key", null, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectWhenQuotaExceeded() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        ingestionProps.setPerTenantAppPerMinuteLimit(1);

        AICallLogIngestionController controller = new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                AICallLogIngestionStore.NO_OP
        );
        AICallLogIngestionController.AICallLogUploadBatchRequest request = buildRequest();

        ResponseEntity<Map<String, Object>> first = controller.ingestBatch("center-key", null, request);
        ResponseEntity<Map<String, Object>> second = controller.ingestBatch("center-key", null, request);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldQueryPersistedRecords() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        MemoryStore store = new MemoryStore();
        store.saveBatch("t-1", "a-1", buildRequest().getItems());
        AICallLogIngestionController controller = new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                store
        );

        ResponseEntity<?> response = controller.queryRecords("center-key", "t-1", "a-1", 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldQueryRecordsByCursor() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        MemoryStore store = new MemoryStore();
        store.saveBatch("t-1", "a-1", List.of(
                AICallLogUploadItem.builder().callId("c1").status("SUCCESS").build(),
                AICallLogUploadItem.builder().callId("c2").status("SUCCESS").build(),
                AICallLogUploadItem.builder().callId("c3").status("SUCCESS").build()
        ));
        AICallLogIngestionController controller = new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                store
        );

        ResponseEntity<?> response = controller.queryRecordsByCursor("center-key", "t-1", "a-1", 0, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("count")).isEqualTo(2);
        assertThat(body.get("hasMore")).isEqualTo(true);
    }

    @Test
    void shouldRejectUnsupportedContractVersion() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        ingestionProps.setSupportedContractVersion("v1");
        AICallLogIngestionController controller = new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                AICallLogIngestionStore.NO_OP
        );

        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("center-key", "v2", buildRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldPurgeExpiredRecords() {
        SfChainLoggingProperties logProps = new SfChainLoggingProperties();
        SfChainIngestionProperties ingestionProps = new SfChainIngestionProperties();
        ingestionProps.setApiKey("center-key");
        AICallLogIngestionStore store = new AICallLogIngestionStore() {
            @Override
            public void saveBatch(String tenantId, String appId, List<AICallLogUploadItem> items) {
            }

            @Override
            public int purgeExpired() {
                return 2;
            }
        };
        AICallLogIngestionController controller = new AICallLogIngestionController(
                new AICallLogManager(logProps),
                ingestionProps,
                new MinuteWindowQuotaService(ingestionProps),
                store
        );

        ResponseEntity<?> response = controller.purgeExpired("center-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static class MemoryStore implements AICallLogIngestionStore {
        private final List<AICallLogIngestionRecord> records = new ArrayList<>();

        @Override
        public void saveBatch(String tenantId, String appId, List<AICallLogUploadItem> items) {
            for (AICallLogUploadItem item : items) {
                records.add(new AICallLogIngestionRecord(tenantId, appId, LocalDateTime.now(), item));
            }
        }

        @Override
        public List<AICallLogIngestionRecord> query(String tenantId, String appId, int limit) {
            return records;
        }

        @Override
        public AICallLogIngestionPage queryPage(String tenantId, String appId, int cursor, int limit) {
            int from = Math.max(cursor, 0);
            int to = Math.min(from + limit, records.size());
            List<AICallLogIngestionRecord> page = records.subList(from, to);
            boolean hasMore = to < records.size();
            Integer nextCursor = hasMore ? to : null;
            return new AICallLogIngestionPage(page, nextCursor, hasMore);
        }
    }
}
