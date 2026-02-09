package com.suifeng.sfchain.configcenter.service;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
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

        ApiKeyDtos.ValidateApiKeyResponse keyCheck = validateBySecret(apiKey, tenantId, appId);
        if (!keyCheck.isValid()) {
            throw new IllegalArgumentException("api key invalid: " + keyCheck.getMessage());
        }

        ConfigDtos.ConfigSnapshotResponse response = new ConfigDtos.ConfigSnapshotResponse();
        response.setTenantId(tenantId);
        response.setAppId(appId);
        response.setGeneratedAt(LocalDateTime.now());
        response.setVersion("v" + System.currentTimeMillis());
        response.setModels(listModelConfigs(tenantId, appId));
        response.setOperations(listOperationConfigs(tenantId, appId));
        return response;
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
