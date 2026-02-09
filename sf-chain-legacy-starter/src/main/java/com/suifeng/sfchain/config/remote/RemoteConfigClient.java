package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * 远程配置中心HTTP客户端
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteConfigClient {

    private final ObjectMapper objectMapper;
    private final SfChainServerProperties serverProperties;

    public Optional<RemoteConfigSnapshot> fetchSnapshot(String currentVersion) throws IOException, InterruptedException {
        if (!StringUtils.hasText(serverProperties.getBaseUrl())) {
            throw new IllegalStateException("sf-chain.server.base-url 未配置");
        }
        if (!StringUtils.hasText(serverProperties.getApiKey())) {
            throw new IllegalStateException("sf-chain.server.api-key 未配置");
        }

        String requestUrl = buildSnapshotUrl(currentVersion);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(serverProperties.getConnectTimeoutMs()))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "application/json")
                .header("X-SF-API-KEY", serverProperties.getApiKey())
                .timeout(Duration.ofMillis(serverProperties.getReadTimeoutMs()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        if (statusCode == 304) {
            return Optional.of(notModifiedSnapshot(currentVersion));
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("配置中心请求失败, status=" + statusCode);
        }

        RemoteConfigSnapshot snapshot = objectMapper.readValue(response.body(), RemoteConfigSnapshot.class);
        if (snapshot == null) {
            return Optional.empty();
        }
        if (snapshot.isNotModified()) {
            return Optional.of(notModifiedSnapshot(currentVersion));
        }
        return Optional.of(snapshot);
    }

    public void pushGovernanceFeedback(String snapshotVersion, GovernanceSyncApplyResult result)
            throws IOException, InterruptedException {
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/feedback";
        postJson(requestUrl, toGovernancePayload(snapshotVersion, result), "治理反馈上报失败");
    }

    public void pushGovernanceEvent(String snapshotVersion, GovernanceSyncApplyResult result)
            throws IOException, InterruptedException {
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/events";
        postJson(requestUrl, toGovernancePayload(snapshotVersion, result), "治理事件上报失败");
    }

    public boolean pushGovernanceFinalize(String snapshotVersion, GovernanceSyncApplyResult result)
            throws IOException, InterruptedException {
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/finalize";
        HttpResponse<String> response =
                postJson(requestUrl, toGovernancePayload(snapshotVersion, result), "治理终态回调失败");
        String body = response.body();
        if (body == null || body.isBlank()) {
            return true;
        }
        try {
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object acknowledged = parsed.get("acknowledged");
            if (acknowledged instanceof Boolean) {
                return (Boolean) acknowledged;
            }
        } catch (Exception ignore) {
            return true;
        }
        return true;
    }

    private String buildSnapshotUrl(String currentVersion) {
        StringBuilder url = new StringBuilder(trimTrailingSlash(serverProperties.getBaseUrl()))
                .append("/v1/config/snapshot?")
                .append("tenantId=").append(encode(serverProperties.getTenantId()))
                .append("&appId=").append(encode(serverProperties.getAppId()));

        if (StringUtils.hasText(currentVersion)) {
            url.append("&version=").append(encode(currentVersion));
        }
        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Map<String, Object> toGovernancePayload(String snapshotVersion, GovernanceSyncApplyResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", serverProperties.getTenantId());
        payload.put("appId", serverProperties.getAppId());
        payload.put("snapshotVersion", snapshotVersion);
        payload.put("releaseId", result.getReleaseId());
        payload.put("stage", result.getStage());
        payload.put("status", result.getStatus() == null ? null : result.getStatus().name());
        payload.put("reasonCode", result.getReasonCode());
        payload.put("nextRetryAtEpochMs", result.getNextRetryAtEpochMs());
        payload.put("eventTimeEpochMs", result.getEventTimeEpochMs());
        payload.put("idempotencyKey", buildIdempotencyKey(result));
        payload.put("valid", result.isValid());
        payload.put("applied", result.isApplied());
        payload.put("targeted", result.isTargeted());
        payload.put("rolledBack", result.isRolledBack());
        payload.put("rebuilt", result.getRebuilt());
        payload.put("sampleCount", result.getSampleCount());
        payload.put("rejectRate", result.getRejectRate());
        payload.put("message", result.getMessage());
        payload.put("requestedVersions", result.getRequestedVersions());
        payload.put("activeVersions", result.getActiveVersions());
        return payload;
    }

    private HttpResponse<String> postJson(String requestUrl, Map<String, Object> payload, String failurePrefix)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(serverProperties.getConnectTimeoutMs()))
                .build();
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-SF-API-KEY", serverProperties.getApiKey())
                .timeout(Duration.ofMillis(serverProperties.getReadTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException(failurePrefix + ", status=" + statusCode);
        }
        return response;
    }

    private String buildIdempotencyKey(GovernanceSyncApplyResult result) {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(serverProperties.getTenantId() == null ? "" : serverProperties.getTenantId());
        joiner.add(serverProperties.getAppId() == null ? "" : serverProperties.getAppId());
        joiner.add(result.getReleaseId() == null ? "" : result.getReleaseId());
        joiner.add(result.getStatus() == null ? "" : result.getStatus().name());
        return joiner.toString();
    }

    private static RemoteConfigSnapshot notModifiedSnapshot(String version) {
        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion(version);
        snapshot.setNotModified(true);
        return snapshot;
    }
}
