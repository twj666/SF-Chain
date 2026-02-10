package com.suifeng.sfchain.core.logging.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 基于HTTP的日志上报客户端
 */
@Slf4j
public class HttpAICallLogUploadClient implements AICallLogUploadClient {

    private final ObjectMapper objectMapper;
    private final SfChainServerProperties serverProperties;
    private final SfChainLoggingProperties loggingProperties;
    private final HttpClient httpClient;

    public HttpAICallLogUploadClient(
            ObjectMapper objectMapper,
            SfChainServerProperties serverProperties,
            SfChainLoggingProperties loggingProperties) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.serverProperties = serverProperties;
        this.loggingProperties = loggingProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(serverProperties.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public boolean upload(List<AICallLogUploadItem> items) {
        try {
            String endpoint = buildEndpointUrl();
            String body = objectMapper.writeValueAsString(new UploadRequest(
                    "v1",
                    serverProperties.getTenantId(),
                    serverProperties.getAppId(),
                    items
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-SF-API-KEY", serverProperties.getApiKey())
                    .timeout(Duration.ofMillis(serverProperties.getReadTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            }
            log.warn("AI调用日志上报失败, status={}", code);
            return false;
        } catch (Exception e) {
            log.warn("AI调用日志上报异常: {}", e.getMessage());
            return false;
        }
    }

    private String buildEndpointUrl() {
        String baseUrl = serverProperties.getBaseUrl();
        String endpoint = loggingProperties.getUploadEndpoint();
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpoint;
        }
        if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }
        return baseUrl + endpoint;
    }

    @lombok.Value
    private static class UploadRequest {
        String contractVersion;
        String tenantId;
        String appId;
        List<AICallLogUploadItem> items;
    }
}
