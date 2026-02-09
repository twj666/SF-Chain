package com.suifeng.sfchain.configcenter.controller;

import com.suifeng.sfchain.configcenter.dto.ApiKeyDtos;
import com.suifeng.sfchain.configcenter.dto.AppDtos;
import com.suifeng.sfchain.configcenter.dto.ConfigDtos;
import com.suifeng.sfchain.configcenter.dto.TenantDtos;
import com.suifeng.sfchain.configcenter.service.ControlPlaneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ControlPlaneController {

    private final ControlPlaneService controlPlaneService;

    @GetMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants")
    public List<TenantDtos.TenantView> listTenants() {
        return controlPlaneService.listTenants();
    }

    @PostMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants")
    public TenantDtos.TenantView createTenant(@RequestBody TenantDtos.CreateTenantRequest request) {
        return controlPlaneService.createTenant(request);
    }

    @PatchMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/status")
    public TenantDtos.TenantView updateTenantStatus(
            @PathVariable String tenantId,
            @RequestBody TenantDtos.UpdateTenantStatusRequest request) {
        return controlPlaneService.updateTenantStatus(tenantId, request.isActive());
    }

    @GetMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps")
    public List<AppDtos.AppView> listApps(@PathVariable String tenantId) {
        return controlPlaneService.listApps(tenantId);
    }

    @PostMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps")
    public AppDtos.AppView createApp(
            @PathVariable String tenantId,
            @RequestBody AppDtos.CreateAppRequest request) {
        return controlPlaneService.createApp(tenantId, request);
    }

    @PatchMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps/{appId}/status")
    public AppDtos.AppView updateAppStatus(
            @PathVariable String tenantId,
            @PathVariable String appId,
            @RequestBody AppDtos.UpdateAppStatusRequest request) {
        return controlPlaneService.updateAppStatus(tenantId, appId, request.isActive());
    }

    @GetMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/api-keys")
    public List<ApiKeyDtos.ApiKeyView> listApiKeys(
            @PathVariable String tenantId,
            @RequestParam(required = false) String appId) {
        return controlPlaneService.listApiKeys(tenantId, appId);
    }

    @PostMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/api-keys")
    public ApiKeyDtos.ApiKeyCreateResponse createApiKey(
            @PathVariable String tenantId,
            @RequestBody ApiKeyDtos.CreateApiKeyRequest request) {
        return controlPlaneService.createApiKey(tenantId, request);
    }

    @PatchMapping("${sf-chain.path.api-prefix:/sf-chain}/control/api-keys/{keyId}/revoke")
    public ApiKeyDtos.ApiKeyView revokeApiKey(@PathVariable Long keyId) {
        return controlPlaneService.revokeApiKey(keyId);
    }

    @GetMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps/{appId}/models")
    public List<Map<String, Object>> listModelConfigs(@PathVariable String tenantId, @PathVariable String appId) {
        return controlPlaneService.listModelConfigs(tenantId, appId);
    }

    @PostMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps/{appId}/models")
    public ConfigDtos.UpsertModelConfigRequest upsertModelConfig(
            @PathVariable String tenantId,
            @PathVariable String appId,
            @RequestBody ConfigDtos.UpsertModelConfigRequest request) {
        return controlPlaneService.upsertModelConfig(tenantId, appId, request);
    }

    @GetMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps/{appId}/operations")
    public List<Map<String, Object>> listOperationConfigs(@PathVariable String tenantId, @PathVariable String appId) {
        return controlPlaneService.listOperationConfigs(tenantId, appId);
    }

    @PostMapping("${sf-chain.path.api-prefix:/sf-chain}/control/tenants/{tenantId}/apps/{appId}/operations")
    public ConfigDtos.UpsertOperationConfigRequest upsertOperationConfig(
            @PathVariable String tenantId,
            @PathVariable String appId,
            @RequestBody ConfigDtos.UpsertOperationConfigRequest request) {
        return controlPlaneService.upsertOperationConfig(tenantId, appId, request);
    }

    @PostMapping("/v1/config/snapshot")
    public ConfigDtos.ConfigSnapshotResponse snapshot(@RequestBody ConfigDtos.ConfigSnapshotRequest request) {
        return controlPlaneService.snapshot(request);
    }

    @PostMapping("/v1/auth/token/validate")
    public ApiKeyDtos.ValidateApiKeyResponse validateApiKey(@RequestBody ApiKeyDtos.ValidateApiKeyRequest request) {
        return controlPlaneService.validateApiKey(request);
    }

    @PostMapping("${sf-chain.path.api-prefix:/sf-chain}/control/auth/token/validate")
    public ApiKeyDtos.ValidateApiKeyResponse validateApiKeyForUi(@RequestBody ApiKeyDtos.ValidateApiKeyRequest request) {
        return controlPlaneService.validateApiKey(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(Map.of("message", ex.getMessage()));
    }
}
