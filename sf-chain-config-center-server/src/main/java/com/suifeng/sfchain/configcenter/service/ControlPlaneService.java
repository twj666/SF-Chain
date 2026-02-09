package com.suifeng.sfchain.configcenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.configcenter.dto.ApiKeyDtos;
import com.suifeng.sfchain.configcenter.dto.AppDtos;
import com.suifeng.sfchain.configcenter.dto.ConfigDtos;
import com.suifeng.sfchain.configcenter.dto.TenantDtos;
import com.suifeng.sfchain.configcenter.entity.ApiKeyEntity;
import com.suifeng.sfchain.configcenter.entity.AppEntity;
import com.suifeng.sfchain.configcenter.entity.TenantEntity;
import com.suifeng.sfchain.configcenter.entity.TenantModelConfigEntity;
import com.suifeng.sfchain.configcenter.entity.TenantOperationConfigEntity;
import com.suifeng.sfchain.configcenter.repository.ApiKeyRepository;
import com.suifeng.sfchain.configcenter.repository.AppRepository;
import com.suifeng.sfchain.configcenter.repository.TenantModelConfigRepository;
import com.suifeng.sfchain.configcenter.repository.TenantOperationConfigRepository;
import com.suifeng.sfchain.configcenter.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ControlPlaneService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final AppRepository appRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final TenantModelConfigRepository tenantModelConfigRepository;
    private final TenantOperationConfigRepository tenantOperationConfigRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TenantDtos.TenantView createTenant(TenantDtos.CreateTenantRequest request) {
        String tenantId = normalizeRequired(request.getTenantId(), "tenantId");
        String name = normalizeRequired(request.getName(), "name");
        if (tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("tenant already exists: " + tenantId);
        }
        TenantEntity entity = new TenantEntity();
        entity.setTenantId(tenantId);
        entity.setName(name);
        entity.setDescription(normalizeOptional(request.getDescription()));
        entity.setActive(true);
        return toTenantView(tenantRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TenantDtos.TenantView> listTenants() {
        return tenantRepository.findAll().stream().map(this::toTenantView).collect(Collectors.toList());
    }

    @Transactional
    public TenantDtos.TenantView updateTenantStatus(String tenantId, boolean active) {
        TenantEntity entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("tenant not found: " + tenantId));
        entity.setActive(active);
        return toTenantView(tenantRepository.save(entity));
    }

    @Transactional
    public AppDtos.AppView createApp(String tenantId, AppDtos.CreateAppRequest request) {
        TenantEntity tenant = assertTenantActive(tenantId);
        String appId = normalizeRequired(request.getAppId(), "appId");
        String appName = normalizeRequired(request.getAppName(), "appName");
        if (appRepository.existsByTenantIdAndAppId(tenant.getTenantId(), appId)) {
            throw new IllegalArgumentException("app already exists: " + appId);
        }
        AppEntity entity = new AppEntity();
        entity.setTenantId(tenant.getTenantId());
        entity.setAppId(appId);
        entity.setAppName(appName);
        entity.setDescription(normalizeOptional(request.getDescription()));
        entity.setActive(true);
        return toAppView(appRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<AppDtos.AppView> listApps(String tenantId) {
        return appRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toAppView)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppDtos.AppView updateAppStatus(String tenantId, String appId, boolean active) {
        AppEntity app = appRepository.findByTenantIdAndAppId(tenantId, appId)
                .orElseThrow(() -> new IllegalArgumentException("app not found: " + appId));
        app.setActive(active);
        return toAppView(appRepository.save(app));
    }

    @Transactional
    public ApiKeyDtos.ApiKeyCreateResponse createApiKey(String tenantId, ApiKeyDtos.CreateApiKeyRequest request) {
        assertTenantActive(tenantId);
        String appId = normalizeRequired(request.getAppId(), "appId");
        assertAppActive(tenantId, appId);
        String keyName = normalizeRequired(request.getKeyName(), "keyName");

        String plainKey = generateApiKey();
        String keyPrefix = plainKey.substring(0, Math.min(14, plainKey.length()));

        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setTenantId(tenantId);
        entity.setAppId(appId);
        entity.setKeyName(keyName);
        entity.setKeyPrefix(keyPrefix);
        entity.setSecretHash(sha256Hex(plainKey));
        entity.setActive(true);

        ApiKeyEntity saved = apiKeyRepository.save(entity);

        ApiKeyDtos.ApiKeyCreateResponse response = new ApiKeyDtos.ApiKeyCreateResponse();
        response.setId(saved.getId());
        response.setTenantId(saved.getTenantId());
        response.setAppId(saved.getAppId());
        response.setKeyName(saved.getKeyName());
        response.setApiKey(plainKey);
        response.setKeyPrefix(saved.getKeyPrefix());
        response.setCreatedAt(saved.getCreatedAt());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDtos.ApiKeyView> listApiKeys(String tenantId, String appId) {
        List<ApiKeyEntity> entities = (appId == null || appId.isBlank())
                ? apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                : apiKeyRepository.findByTenantIdAndAppIdOrderByCreatedAtDesc(tenantId, appId);
        return entities.stream().map(this::toApiKeyView).collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyDtos.ApiKeyView revokeApiKey(Long keyId) {
        ApiKeyEntity entity = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("api key not found: " + keyId));
        entity.setActive(false);
        return toApiKeyView(apiKeyRepository.save(entity));
    }

    @Transactional
    public ConfigDtos.UpsertModelConfigRequest upsertModelConfig(
            String tenantId,
            String appId,
            ConfigDtos.UpsertModelConfigRequest request) {
        assertTenantActive(tenantId);
        assertAppActive(tenantId, appId);

        String modelName = normalizeRequired(request.getModelName(), "modelName");
        String provider = normalizeRequired(request.getProvider(), "provider");

        TenantModelConfigEntity entity = tenantModelConfigRepository
                .findByTenantIdAndAppIdAndModelName(tenantId, appId, modelName)
                .orElseGet(TenantModelConfigEntity::new);

        entity.setTenantId(tenantId);
        entity.setAppId(appId);
        entity.setModelName(modelName);
        entity.setProvider(provider);
        entity.setBaseUrl(normalizeOptional(request.getBaseUrl()));
        entity.setActive(request.isActive());
        entity.setConfigJson(request.getConfig() == null ? Map.of() : request.getConfig());

        tenantModelConfigRepository.save(entity);
        return request;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listModelConfigs(String tenantId, String appId) {
        assertTenantActive(tenantId);
        assertAppActive(tenantId, appId);
        return tenantModelConfigRepository.findByTenantIdAndAppIdOrderByCreatedAtDesc(tenantId, appId).stream()
                .map(item -> Map.<String, Object>of(
                        "modelName", item.getModelName(),
                        "provider", item.getProvider(),
                        "baseUrl", item.getBaseUrl() == null ? "" : item.getBaseUrl(),
                        "active", item.isActive(),
                        "config", item.getConfigJson() == null ? Map.of() : item.getConfigJson(),
                        "updatedAt", item.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> testModelConfig(String tenantId, String appId, String modelName) {
        assertTenantActive(tenantId);
        assertAppActive(tenantId, appId);

        TenantModelConfigEntity model = tenantModelConfigRepository
                .findByTenantIdAndAppIdAndModelName(tenantId, appId, modelName)
                .orElseThrow(() -> new IllegalArgumentException("model not found: " + modelName));
        if (!model.isActive()) {
            throw new IllegalStateException("model disabled: " + modelName);
        }

        String baseUrl = normalizeRequired(model.getBaseUrl(), "baseUrl");
        String endpoint = normalizeChatCompletionsEndpoint(baseUrl);
        Map<String, Object> config = model.getConfigJson() == null ? Map.of() : model.getConfigJson();
        String apiKey = normalizeRequired(toString(config.get("apiKey")), "apiKey");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model.getModelName());
        payload.put("messages", List.of(Map.of("role", "user", "content", "Reply with OK only.")));
        payload.put("max_tokens", 8);

        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            long durationMs = System.currentTimeMillis() - start;
            boolean success = statusCode >= 200 && statusCode < 300;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", success);
            result.put("modelName", model.getModelName());
            result.put("provider", model.getProvider());
            result.put("statusCode", statusCode);
            result.put("durationMs", durationMs);
            result.put("endpoint", endpoint);
            result.put("message", success ? "模型测试成功" : "模型测试失败");
            result.put("responsePreview", abbreviate(response.body(), 500));
            return result;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("modelName", model.getModelName());
            result.put("provider", model.getProvider());
            result.put("durationMs", durationMs);
            result.put("endpoint", endpoint);
            result.put("message", "模型测试失败: " + ex.getMessage());
            return result;
        }
    }

    @Transactional
    public ConfigDtos.UpsertOperationConfigRequest upsertOperationConfig(
            String tenantId,
            String appId,
            ConfigDtos.UpsertOperationConfigRequest request) {
        assertTenantActive(tenantId);
        assertAppActive(tenantId, appId);

        String operationType = normalizeRequired(request.getOperationType(), "operationType");

        TenantOperationConfigEntity entity = tenantOperationConfigRepository
                .findByTenantIdAndAppIdAndOperationType(tenantId, appId, operationType)
                .orElseGet(TenantOperationConfigEntity::new);

        entity.setTenantId(tenantId);
        entity.setAppId(appId);
        entity.setOperationType(operationType);
        entity.setModelName(normalizeOptional(request.getModelName()));
        entity.setActive(request.isActive());
        entity.setConfigJson(request.getConfig() == null ? Map.of() : request.getConfig());

        tenantOperationConfigRepository.save(entity);
        return request;
    }

    @Transactional
    public ConfigDtos.OperationCatalogSyncResponse syncOperationCatalog(
            String tenantId,
            String appId,
            String apiKey,
            ConfigDtos.OperationCatalogSyncRequest request) {
        String normalizedTenantId = normalizeRequired(tenantId, "tenantId");
        String normalizedAppId = normalizeRequired(appId, "appId");
        String normalizedApiKey = normalizeRequired(apiKey, "apiKey");
        ApiKeyDtos.ValidateApiKeyResponse keyCheck = validateBySecret(normalizedApiKey, normalizedTenantId, normalizedAppId);
        if (!keyCheck.isValid()) {
            throw new IllegalArgumentException("api key invalid: " + keyCheck.getMessage());
        }
        assertTenantActive(normalizedTenantId);
        assertAppActive(normalizedTenantId, normalizedAppId);

        List<ConfigDtos.OperationCatalogItem> operations =
                request == null || request.getOperations() == null ? List.of() : request.getOperations();
        Map<String, TenantOperationConfigEntity> existing = tenantOperationConfigRepository
                .findByTenantIdAndAppIdOrderByCreatedAtDesc(normalizedTenantId, normalizedAppId)
                .stream()
                .collect(Collectors.toMap(TenantOperationConfigEntity::getOperationType, it -> it, (a, b) -> a));

        int created = 0;
        int existed = 0;
        int ignored = 0;
        for (ConfigDtos.OperationCatalogItem item : operations) {
            if (item == null || normalizeOptional(item.getOperationType()) == null) {
                ignored++;
                continue;
            }
            String operationType = normalizeRequired(item.getOperationType(), "operationType");
            if (existing.containsKey(operationType)) {
                existed++;
                continue;
            }
            TenantOperationConfigEntity entity = new TenantOperationConfigEntity();
            entity.setTenantId(normalizedTenantId);
            entity.setAppId(normalizedAppId);
            entity.setOperationType(operationType);
            entity.setActive(item.isEnabled());
            entity.setModelName(normalizeOptional(item.getDefaultModel()));
            entity.setConfigJson(toCatalogMeta(item));
            TenantOperationConfigEntity saved = tenantOperationConfigRepository.save(entity);
            existing.put(saved.getOperationType(), saved);
            created++;
        }

        ConfigDtos.OperationCatalogSyncResponse response = new ConfigDtos.OperationCatalogSyncResponse();
        response.setTenantId(normalizedTenantId);
        response.setAppId(normalizedAppId);
        response.setReceived(operations.size());
        response.setCreated(created);
        response.setExisted(existed);
        response.setIgnored(ignored);
        response.setSyncedAt(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOperationConfigs(String tenantId, String appId) {
        assertTenantActive(tenantId);
        assertAppActive(tenantId, appId);
        return tenantOperationConfigRepository.findByTenantIdAndAppIdOrderByCreatedAtDesc(tenantId, appId).stream()
                .map(item -> Map.<String, Object>of(
                        "operationType", item.getOperationType(),
                        "modelName", item.getModelName() == null ? "" : item.getModelName(),
                        "active", item.isActive(),
                        "config", item.getConfigJson() == null ? Map.of() : item.getConfigJson(),
                        "updatedAt", item.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyDtos.ValidateApiKeyResponse validateApiKey(ApiKeyDtos.ValidateApiKeyRequest request) {
        String apiKey = normalizeRequired(request.getApiKey(), "apiKey");
        String tenantId = normalizeOptional(request.getTenantId());
        String appId = normalizeOptional(request.getAppId());

        ApiKeyDtos.ValidateApiKeyResponse response = validateBySecret(apiKey, tenantId, appId);
        if (response.isValid()) {
            ApiKeyEntity entity = findApiKeyEntity(apiKey, tenantId, appId).orElse(null);
            if (entity != null) {
                entity.setLastUsedAt(LocalDateTime.now());
                apiKeyRepository.save(entity);
            }
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ConfigDtos.ConfigSnapshotResponse snapshot(ConfigDtos.ConfigSnapshotRequest request) {
        String tenantId = normalizeRequired(request.getTenantId(), "tenantId");
        String appId = normalizeRequired(request.getAppId(), "appId");
        String apiKey = normalizeRequired(request.getApiKey(), "apiKey");
        return buildSnapshotResponse(tenantId, appId, apiKey);
    }

    @Transactional(readOnly = true)
    public Optional<ConfigDtos.ConfigSnapshotResponse> snapshot(
            String tenantId,
            String appId,
            String apiKey,
            String currentVersion) {
        ConfigDtos.ConfigSnapshotResponse response = buildSnapshotResponse(
                normalizeRequired(tenantId, "tenantId"),
                normalizeRequired(appId, "appId"),
                normalizeRequired(apiKey, "apiKey"));
        String normalizedCurrentVersion = normalizeOptional(currentVersion);
        if (StringUtils.hasText(normalizedCurrentVersion) && normalizedCurrentVersion.equals(response.getVersion())) {
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private ConfigDtos.ConfigSnapshotResponse buildSnapshotResponse(String tenantId, String appId, String apiKey) {
        ApiKeyDtos.ValidateApiKeyResponse keyCheck = validateBySecret(apiKey, tenantId, appId);
        if (!keyCheck.isValid()) {
            throw new IllegalArgumentException("api key invalid: " + keyCheck.getMessage());
        }

        List<Map<String, Object>> models = listModelConfigs(tenantId, appId);
        List<Map<String, Object>> operations = listOperationConfigs(tenantId, appId);
        ConfigDtos.ConfigSnapshotResponse response = new ConfigDtos.ConfigSnapshotResponse();
        response.setTenantId(tenantId);
        response.setAppId(appId);
        response.setGeneratedAt(LocalDateTime.now());
        response.setVersion(buildSnapshotVersion(models, operations));
        response.setModels(models);
        response.setOperations(operations);
        return response;
    }

    private static String buildSnapshotVersion(List<Map<String, Object>> models, List<Map<String, Object>> operations) {
        long latestUpdatedAt = 0L;
        for (Map<String, Object> model : models) {
            Object updatedAt = model.get("updatedAt");
            latestUpdatedAt = Math.max(latestUpdatedAt, toEpochMillis(updatedAt));
        }
        for (Map<String, Object> operation : operations) {
            Object updatedAt = operation.get("updatedAt");
            latestUpdatedAt = Math.max(latestUpdatedAt, toEpochMillis(updatedAt));
        }
        return "v" + latestUpdatedAt + "_m" + models.size() + "_o" + operations.size();
    }

    private static long toEpochMillis(Object updatedAt) {
        if (updatedAt instanceof LocalDateTime) {
            return ((LocalDateTime) updatedAt).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return 0L;
    }

    private static Map<String, Object> toCatalogMeta(ConfigDtos.OperationCatalogItem item) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (normalizeOptional(item.getSourceClass()) != null) {
            meta.put("sourceClass", item.getSourceClass().trim());
        }
        if (normalizeOptional(item.getDescription()) != null) {
            meta.put("description", item.getDescription().trim());
        }
        if (normalizeOptional(item.getDefaultModel()) != null) {
            meta.put("defaultModel", item.getDefaultModel().trim());
        }
        meta.put("requireJsonOutput", item.isRequireJsonOutput());
        meta.put("supportThinking", item.isSupportThinking());
        if (item.getDefaultMaxTokens() > 0) {
            meta.put("defaultMaxTokens", item.getDefaultMaxTokens());
        }
        if (item.getDefaultTemperature() >= 0) {
            meta.put("defaultTemperature", item.getDefaultTemperature());
        }
        List<String> supportedModels = item.getSupportedModels() == null ? List.of() : item.getSupportedModels();
        List<String> cleanedModels = new ArrayList<>();
        for (String supportedModel : supportedModels) {
            String model = normalizeOptional(supportedModel);
            if (model != null) {
                cleanedModels.add(model);
            }
        }
        if (!cleanedModels.isEmpty()) {
            meta.put("supportedModels", cleanedModels);
        }
        if (meta.isEmpty()) {
            return Map.of();
        }
        return Map.of("_catalog", meta);
    }

    private ApiKeyDtos.ValidateApiKeyResponse validateBySecret(String apiKey, String tenantId, String appId) {
        String keyPrefix = apiKey.substring(0, Math.min(14, apiKey.length()));
        List<ApiKeyEntity> candidates = apiKeyRepository.findByKeyPrefix(keyPrefix);
        String hash = sha256Hex(apiKey);

        Optional<ApiKeyEntity> matched = candidates.stream()
                .filter(ApiKeyEntity::isActive)
                .filter(item -> item.getSecretHash().equals(hash))
                .filter(item -> tenantId == null || tenantId.equals(item.getTenantId()))
                .filter(item -> appId == null || appId.equals(item.getAppId()))
                .findFirst();

        ApiKeyDtos.ValidateApiKeyResponse response = new ApiKeyDtos.ValidateApiKeyResponse();
        if (matched.isEmpty()) {
            response.setValid(false);
            response.setMessage("invalid api key");
            return response;
        }

        ApiKeyEntity entity = matched.get();
        try {
            assertTenantActive(entity.getTenantId());
            assertAppActive(entity.getTenantId(), entity.getAppId());
        } catch (RuntimeException ex) {
            response.setValid(false);
            response.setTenantId(entity.getTenantId());
            response.setAppId(entity.getAppId());
            response.setMessage(ex.getMessage());
            return response;
        }

        response.setValid(true);
        response.setTenantId(entity.getTenantId());
        response.setAppId(entity.getAppId());
        response.setMessage("ok");
        return response;
    }

    private Optional<ApiKeyEntity> findApiKeyEntity(String apiKey, String tenantId, String appId) {
        String keyPrefix = apiKey.substring(0, Math.min(14, apiKey.length()));
        String hash = sha256Hex(apiKey);
        return apiKeyRepository.findByKeyPrefix(keyPrefix).stream()
                .filter(item -> item.getSecretHash().equals(hash))
                .filter(item -> tenantId == null || tenantId.equals(item.getTenantId()))
                .filter(item -> appId == null || appId.equals(item.getAppId()))
                .findFirst();
    }

    private TenantEntity assertTenantActive(String tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("tenant not found: " + tenantId));
        if (!tenant.isActive()) {
            throw new IllegalStateException("tenant disabled: " + tenantId);
        }
        return tenant;
    }

    private AppEntity assertAppActive(String tenantId, String appId) {
        AppEntity app = appRepository.findByTenantIdAndAppId(tenantId, appId)
                .orElseThrow(() -> new IllegalArgumentException("app not found: " + appId));
        if (!app.isActive()) {
            throw new IllegalStateException("app disabled: " + appId);
        }
        return app;
    }

    private TenantDtos.TenantView toTenantView(TenantEntity entity) {
        TenantDtos.TenantView view = new TenantDtos.TenantView();
        view.setTenantId(entity.getTenantId());
        view.setName(entity.getName());
        view.setDescription(entity.getDescription());
        view.setActive(entity.isActive());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }

    private AppDtos.AppView toAppView(AppEntity entity) {
        AppDtos.AppView view = new AppDtos.AppView();
        view.setId(entity.getId());
        view.setTenantId(entity.getTenantId());
        view.setAppId(entity.getAppId());
        view.setAppName(entity.getAppName());
        view.setDescription(entity.getDescription());
        view.setActive(entity.isActive());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }

    private ApiKeyDtos.ApiKeyView toApiKeyView(ApiKeyEntity entity) {
        ApiKeyDtos.ApiKeyView view = new ApiKeyDtos.ApiKeyView();
        view.setId(entity.getId());
        view.setTenantId(entity.getTenantId());
        view.setAppId(entity.getAppId());
        view.setKeyName(entity.getKeyName());
        view.setKeyPrefix(entity.getKeyPrefix());
        view.setActive(entity.isActive());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        view.setLastUsedAt(entity.getLastUsedAt());
        return view;
    }

    private static String normalizeRequired(String raw, String field) {
        String value = normalizeOptional(raw);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String normalizeOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private static String toString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static String normalizeChatCompletionsEndpoint(String baseUrl) {
        String value = baseUrl.trim();
        if (value.endsWith("/chat/completions")) {
            return value;
        }
        if (value.endsWith("/")) {
            return value + "chat/completions";
        }
        if (value.endsWith("/v1") || value.endsWith("/v1/")) {
            return value + (value.endsWith("/") ? "chat/completions" : "/chat/completions");
        }
        return value;
    }

    private static String abbreviate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    private static String generateApiKey() {
        byte[] random = new byte[24];
        SECURE_RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        return "sk_sf_" + token;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("sha-256 unavailable", e);
        }
    }
}
