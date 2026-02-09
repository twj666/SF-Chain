package com.suifeng.sfchain.configcenter.dto;

import lombok.Data;

import java.time.LocalDateTime;

public final class TenantDtos {

    private TenantDtos() {
    }

    @Data
    public static class CreateTenantRequest {
        private String tenantId;
        private String name;
        private String description;
    }

    @Data
    public static class TenantView {
        private String tenantId;
        private String name;
        private String description;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class UpdateTenantStatusRequest {
        private boolean active;
    }
}
