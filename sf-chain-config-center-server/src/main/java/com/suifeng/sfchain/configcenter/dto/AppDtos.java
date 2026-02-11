package com.suifeng.sfchain.configcenter.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class AppDtos {

    private AppDtos() {
    }

    @Data
    public static class CreateAppRequest {
        private String appId;
        private String appName;
        private String description;
    }

    @Data
    public static class AppView {
        private Long id;
        private String tenantId;
        private String appId;
        private String appName;
        private String description;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class UpdateAppStatusRequest {
        private boolean active;
    }

    @Data
    public static class OnlineAppView {
        private String tenantId;
        private String tenantName;
        private String appId;
        private String appName;
        private boolean online;
        private long instanceCount;
        private LocalDateTime lastSeenAt;
        private long offlineSeconds;
        private List<OnlineInstanceView> instances;
    }

    @Data
    public static class OnlineInstanceView {
        private String instanceId;
        private LocalDateTime lastSeenAt;
    }
}
