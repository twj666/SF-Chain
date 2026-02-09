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
import java.util.Optional;

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

    private static RemoteConfigSnapshot notModifiedSnapshot(String version) {
        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion(version);
        snapshot.setNotModified(true);
        return snapshot;
    }
}
