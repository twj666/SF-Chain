package com.suifeng.sfchain.configcenter.repository;

import com.suifeng.sfchain.configcenter.entity.TenantModelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantModelConfigRepository extends JpaRepository<TenantModelConfigEntity, Long> {

    List<TenantModelConfigEntity> findByTenantIdAndAppIdOrderByCreatedAtDesc(String tenantId, String appId);

    Optional<TenantModelConfigEntity> findByTenantIdAndAppIdAndModelName(String tenantId, String appId, String modelName);
}
