package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainServerProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 远程配置中心HTTP客户端
 */
@Slf4j
public class RemoteConfigClient {

    private final ObjectMapper objectMapper;
    private final SfChainServerProperties serverProperties;
    private final LongSupplier currentTimeSupplier;
    private final ConcurrentMap<String, Long> seenResponseSignatures = new ConcurrentHashMap<>();

    public RemoteConfigClient(ObjectMapper objectMapper, SfChainServerProperties serverProperties) {
        this(objectMapper, serverProperties, System::currentTimeMillis);
    }

    RemoteConfigClient(
            ObjectMapper objectMapper,
            SfChainServerProperties serverProperties,
            LongSupplier currentTimeSupplier) {
        this.objectMapper = objectMapper;
        this.serverProperties = serverProperties;
        this.currentTimeSupplier = currentTimeSupplier;
    }

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
                .header("X-SF-INSTANCE-ID", resolveInstanceId())
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
        verifyResponseSignature(response, response.body());

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

    public GovernanceFinalizeAck pushGovernanceFinalize(String snapshotVersion, GovernanceSyncApplyResult result)
            throws IOException, InterruptedException {
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/finalize";
        HttpResponse<String> response =
                postJson(requestUrl, toGovernancePayload(snapshotVersion, result), "治理终态回调失败");
        return parseFinalizeAck(response.body());
    }

    public Optional<String> tryAcquireGovernanceLease(String owner, int ttlSeconds)
            throws IOException, InterruptedException {
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/lease/acquire";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", serverProperties.getTenantId());
        payload.put("appId", serverProperties.getAppId());
        payload.put("owner", owner);
        payload.put("ttlSeconds", ttlSeconds);
        HttpResponse<String> response = postJson(requestUrl, payload, "治理租约获取失败");
        if (response.body() == null || response.body().isBlank()) {
            return Optional.empty();
        }
        Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
        Object acquired = parsed.get("acquired");
        if (!(acquired instanceof Boolean) || !((Boolean) acquired)) {
            return Optional.empty();
        }
        Object token = parsed.get("leaseToken");
        if (token instanceof String && !((String) token).isBlank()) {
            return Optional.of(((String) token).trim());
        }
        return Optional.empty();
    }

    public void releaseGovernanceLease(String leaseToken) throws IOException, InterruptedException {
        if (leaseToken == null || leaseToken.isBlank()) {
            return;
        }
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/lease/release";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", serverProperties.getTenantId());
        payload.put("appId", serverProperties.getAppId());
        payload.put("leaseToken", leaseToken);
        HttpResponse<String> response = postJson(requestUrl, payload, "治理租约释放失败", 404, 409);
        int status = response.statusCode();
        if (status == 404 || status == 409) {
            log.debug("治理租约释放幂等跳过, status={}", status);
        }
    }

    public Optional<GovernanceFinalizeReconcileSnapshot> fetchFinalizeReconciliation()
            throws IOException, InterruptedException {
        return fetchFinalizeReconciliation(null);
    }

    public Optional<GovernanceFinalizeReconcileSnapshot> fetchFinalizeReconciliation(String cursor)
            throws IOException, InterruptedException {
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl()) + "/v1/config/governance/finalize/reconcile?"
                + "tenantId=" + encode(serverProperties.getTenantId())
                + "&appId=" + encode(serverProperties.getAppId());
        if (StringUtils.hasText(cursor)) {
            requestUrl = requestUrl + "&cursor=" + encode(cursor.trim());
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(serverProperties.getConnectTimeoutMs()))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "application/json")
                .header("X-SF-API-KEY", serverProperties.getApiKey())
                .header("X-SF-INSTANCE-ID", resolveInstanceId())
                .timeout(Duration.ofMillis(serverProperties.getReadTimeoutMs()))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        if (statusCode == 404) {
            return Optional.empty();
        }
        if (statusCode == 400 || statusCode == 422) {
            throw new InvalidReconcileCursorException("finalize对账游标无效, status=" + statusCode);
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("finalize对账拉取失败, status=" + statusCode);
        }
        verifyResponseSignature(response, response.body());
        if (response.body() == null || response.body().isBlank()) {
            return Optional.empty();
        }
        GovernanceFinalizeReconcileSnapshot snapshot =
                objectMapper.readValue(response.body(), GovernanceFinalizeReconcileSnapshot.class);
        return Optional.ofNullable(snapshot);
    }

    public void pushOperationCatalog(List<OperationCatalogItem> operations) throws IOException, InterruptedException {
        if (!StringUtils.hasText(serverProperties.getBaseUrl())) {
            throw new IllegalStateException("sf-chain.server.base-url not configured");
        }
        if (!StringUtils.hasText(serverProperties.getApiKey())) {
            throw new IllegalStateException("sf-chain.server.api-key not configured");
        }
        String requestUrl = trimTrailingSlash(serverProperties.getBaseUrl())
                + "/v1/config/operations/catalog?tenantId=" + encode(serverProperties.getTenantId())
                + "&appId=" + encode(serverProperties.getAppId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operations", operations == null ? List.of() : operations);
        postJson(requestUrl, payload, "鎿嶄綔鑺傜偣鐩綍涓婃姤澶辫触");
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

    private String resolveInstanceId() {
        if (StringUtils.hasText(serverProperties.getInstanceId())) {
            return serverProperties.getInstanceId().trim();
        }
        return "unknown-instance";
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

    private HttpResponse<String> postJson(String requestUrl, Map<String, Object> payload, String failurePrefix, int... toleratedStatus)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(serverProperties.getConnectTimeoutMs()))
                .build();
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-SF-API-KEY", serverProperties.getApiKey())
                .header("X-SF-INSTANCE-ID", resolveInstanceId())
                .timeout(Duration.ofMillis(serverProperties.getReadTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        applySignatureHeaders(builder, body);
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        if (!isSuccessOrTolerated(statusCode, toleratedStatus)) {
            throw new IllegalStateException(failurePrefix + ", status=" + statusCode);
        }
        if (statusCode >= 200 && statusCode < 300) {
            verifyResponseSignature(response, response.body());
        }
        return response;
    }

    private static boolean isSuccessOrTolerated(int statusCode, int... toleratedStatus) {
        if (statusCode >= 200 && statusCode < 300) {
            return true;
        }
        if (toleratedStatus == null || toleratedStatus.length == 0) {
            return false;
        }
        for (int status : toleratedStatus) {
            if (statusCode == status) {
                return true;
            }
        }
        return false;
    }

    private String buildIdempotencyKey(GovernanceSyncApplyResult result) {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(serverProperties.getTenantId() == null ? "" : serverProperties.getTenantId());
        joiner.add(serverProperties.getAppId() == null ? "" : serverProperties.getAppId());
        joiner.add(result.getReleaseId() == null ? "" : result.getReleaseId());
        joiner.add(result.getStatus() == null ? "" : result.getStatus().name());
        return joiner.toString();
    }

    private GovernanceFinalizeAck parseFinalizeAck(String body) {
        GovernanceFinalizeAck ack = new GovernanceFinalizeAck();
        if (body == null || body.isBlank()) {
            ack.setAcknowledged(true);
            return ack;
        }
        try {
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object acknowledged = parsed.get("acknowledged");
            if (acknowledged instanceof Boolean) {
                ack.setAcknowledged((Boolean) acknowledged);
            }
            Object ackId = parsed.get("ackId");
            if (ackId instanceof String) {
                ack.setAckId((String) ackId);
            }
            Object ackVersion = parsed.get("ackVersion");
            if (ackVersion instanceof Number) {
                ack.setAckVersion(((Number) ackVersion).longValue());
            }
            Object serverTime = parsed.get("serverTimeEpochMs");
            if (serverTime instanceof Number) {
                ack.setServerTimeEpochMs(((Number) serverTime).longValue());
            }
        } catch (Exception ignore) {
            ack.setAcknowledged(true);
        }
        return ack;
    }

    private void applySignatureHeaders(HttpRequest.Builder builder, String body) {
        if (!serverProperties.isCallbackSignatureEnabled()) {
            return;
        }
        String secret = serverProperties.getCallbackSigningSecret();
        if (!StringUtils.hasText(secret)) {
            return;
        }
        long timestamp = System.currentTimeMillis();
        String payload = timestamp + "\n" + (body == null ? "" : body);
        builder.header("X-SF-SIGNATURE-TS", String.valueOf(timestamp));
        builder.header("X-SF-SIGNATURE", hmacSha256Hex(secret, payload));
    }

    private static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("签名计算失败", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private void verifyResponseSignature(HttpResponse<String> response, String body) {
        if (!serverProperties.isResponseSignatureEnabled()) {
            return;
        }
        String secret = resolveResponseSignatureSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("响应签名校验已启用，但未配置签名密钥");
        }
        String timestampHeader = response.headers().firstValue("X-SF-SIGNATURE-TS").orElse(null);
        String signatureHeader = response.headers().firstValue("X-SF-SIGNATURE").orElse(null);
        if (!StringUtils.hasText(timestampHeader) || !StringUtils.hasText(signatureHeader)) {
            throw new IllegalStateException("响应签名缺失");
        }
        long timestamp = parseTimestamp(timestampHeader);
        long now = currentTimeSupplier.getAsLong();
        long maxSkewMs = Math.max(serverProperties.getResponseSignatureMaxSkewSeconds(), 1) * 1000L;
        if (Math.abs(now - timestamp) > maxSkewMs) {
            throw new IllegalStateException("响应签名时间戳超出允许窗口");
        }
        long replayWindowMs = Math.max(serverProperties.getResponseSignatureReplayWindowSeconds(), 1) * 1000L;
        purgeExpiredSignatureKeys(now - replayWindowMs);
        String replayKey = timestamp + ":" + signatureHeader.trim();
        if (seenResponseSignatures.putIfAbsent(replayKey, now) != null) {
            throw new IllegalStateException("检测到响应重放");
        }
        String payload = timestamp + "\n" + (body == null ? "" : body);
        String expected = hmacSha256Hex(secret, payload);
        if (!constantTimeEquals(expected, signatureHeader.trim())) {
            seenResponseSignatures.remove(replayKey);
            throw new IllegalStateException("响应签名校验失败");
        }
    }

    private String resolveResponseSignatureSecret() {
        if (StringUtils.hasText(serverProperties.getResponseSigningSecret())) {
            return serverProperties.getResponseSigningSecret().trim();
        }
        return serverProperties.getCallbackSigningSecret();
    }

    private static long parseTimestamp(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new IllegalStateException("响应签名时间戳格式非法");
        }
    }

    private void purgeExpiredSignatureKeys(long expireBefore) {
        seenResponseSignatures.entrySet().removeIf(entry -> entry.getValue() < expireBefore);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private static RemoteConfigSnapshot notModifiedSnapshot(String version) {
        RemoteConfigSnapshot snapshot = new RemoteConfigSnapshot();
        snapshot.setVersion(version);
        snapshot.setNotModified(true);
        return snapshot;
    }

    public static class OperationCatalogItem {
        private String operationType;
        private String sourceClass;
        private String description;
        private String defaultModel;
        private boolean enabled;
        private boolean requireJsonOutput;
        private boolean supportThinking;
        private int defaultMaxTokens;
        private double defaultTemperature;
        private List<String> supportedModels;

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getSourceClass() {
            return sourceClass;
        }

        public void setSourceClass(String sourceClass) {
            this.sourceClass = sourceClass;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequireJsonOutput() {
            return requireJsonOutput;
        }

        public void setRequireJsonOutput(boolean requireJsonOutput) {
            this.requireJsonOutput = requireJsonOutput;
        }

        public boolean isSupportThinking() {
            return supportThinking;
        }

        public void setSupportThinking(boolean supportThinking) {
            this.supportThinking = supportThinking;
        }

        public int getDefaultMaxTokens() {
            return defaultMaxTokens;
        }

        public void setDefaultMaxTokens(int defaultMaxTokens) {
            this.defaultMaxTokens = defaultMaxTokens;
        }

        public double getDefaultTemperature() {
            return defaultTemperature;
        }

        public void setDefaultTemperature(double defaultTemperature) {
            this.defaultTemperature = defaultTemperature;
        }

        public List<String> getSupportedModels() {
            return supportedModels;
        }

        public void setSupportedModels(List<String> supportedModels) {
            this.supportedModels = supportedModels;
        }
    }
}
