package com.suifeng.sfchain.core.logging.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpAICallLogUploadClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldUploadBatchWithApiKeyHeader() throws Exception {
        AtomicInteger status = new AtomicInteger();
        AtomicReference<String> apiKeyHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/logs/ai-calls/batch", exchange -> {
            status.set(200);
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("X-SF-API-KEY"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        serverProperties.setApiKey("center-key");
        serverProperties.setTenantId("tenant-a");
        serverProperties.setAppId("app-a");

        SfChainLoggingProperties loggingProperties = new SfChainLoggingProperties();
        loggingProperties.setUploadEndpoint("/v1/logs/ai-calls/batch");

        HttpAICallLogUploadClient client = new HttpAICallLogUploadClient(
                new ObjectMapper(),
                serverProperties,
                loggingProperties
        );

        AICallLogUploadItem item = AICallLogUploadItem.builder()
                .callId("call-http-1")
                .operationType("TEST_OP")
                .modelName("test-model")
                .callTime(LocalDateTime.now())
                .duration(9L)
                .status("SUCCESS")
                .build();

        boolean ok = client.upload(List.of(item));

        assertThat(ok).isTrue();
        assertThat(status.get()).isEqualTo(200);
        assertThat(apiKeyHeader.get()).isEqualTo("center-key");
        assertThat(requestBody.get()).contains("call-http-1");
        assertThat(requestBody.get()).contains("tenant-a");
        assertThat(requestBody.get()).contains("app-a");
    }
}
