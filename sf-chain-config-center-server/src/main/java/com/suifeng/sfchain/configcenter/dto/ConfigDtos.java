package com.suifeng.sfchain.configcenter.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ConfigDtos {

    private ConfigDtos() {
    }

    @Data
    public static class UpsertModelConfigRequest {
        private String modelName;
        private String provider;
        private String baseUrl;
        private boolean active = true;
        private Map<String, Object> config;
    }

    @Data
    public static class UpsertOperationConfigRequest {
        private String operationType;
        private String modelName;
        private boolean active = true;
        private Map<String, Object> config;
    }

    @Data
    public static class ConfigSnapshotRequest {
        private String tenantId;
        private String appId;
        private String apiKey;
    }

    @Data
    public static class ConfigSnapshotResponse {
        private String tenantId;
        private String appId;
        private String version;
        private LocalDateTime generatedAt;
        private Map<String, Object> models;
        private Map<String, Object> operationConfigs;
        private Map<String, String> operationModelMapping;
        private Object operations;
    }

    @Data
    public static class OperationCatalogSyncRequest {
        private List<OperationCatalogItem> operations;
    }

    @Data
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
    }

    @Data
    public static class OperationCatalogSyncResponse {
        private String tenantId;
        private String appId;
        private int received;
        private int created;
        private int existed;
        private int ignored;
        private LocalDateTime syncedAt;
    }
}
