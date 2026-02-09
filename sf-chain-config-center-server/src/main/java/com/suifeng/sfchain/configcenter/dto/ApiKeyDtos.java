package com.suifeng.sfchain.configcenter.dto;

import lombok.Data;

import java.time.LocalDateTime;

public final class ApiKeyDtos {

    private ApiKeyDtos() {
    }

    @Data
    public static class CreateApiKeyRequest {
        private String appId;
        private String keyName;
    }

    @Data
    public static class ApiKeyCreateResponse {
        private Long id;
        private String tenantId;
        private String appId;
        private String keyName;
        private String apiKey;
        private String keyPrefix;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ApiKeyView {
        private Long id;
        private String tenantId;
        private String appId;
        private String keyName;
        private String keyPrefix;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastUsedAt;
    }

    @Data
    public static class ValidateApiKeyRequest {
        private String apiKey;
        private String tenantId;
        private String appId;
    }

    @Data
    public static class ValidateApiKeyResponse {
        private boolean valid;
        private String tenantId;
        private String appId;
        private String message;
    }
}
