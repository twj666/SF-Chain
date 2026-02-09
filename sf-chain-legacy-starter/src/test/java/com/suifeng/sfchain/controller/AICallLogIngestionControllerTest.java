package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AICallLogIngestionControllerTest {

    @Test
    void shouldRejectRequestWhenApiKeyInvalid() {
        AICallLogIngestionController controller = newController("center-key");
        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("wrong-key", buildRequest());

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

        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("center-key", buildRequest());

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

        ResponseEntity<Map<String, Object>> response = controller.ingestBatch("center-key", request);
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

        ResponseEntity<Map<String, Object>> first = controller.ingestBatch("center-key", request);
        ResponseEntity<Map<String, Object>> second = controller.ingestBatch("center-key", request);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
