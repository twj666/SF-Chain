package com.suifeng.sfchain.configcenter.dto;

import lombok.Data;

import java.time.LocalDateTime;
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
        private Object models;
        private Object operations;
    }
}
