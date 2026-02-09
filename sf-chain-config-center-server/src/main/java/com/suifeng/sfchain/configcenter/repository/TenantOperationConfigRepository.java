package com.suifeng.sfchain.configcenter.repository;

import com.suifeng.sfchain.configcenter.entity.TenantOperationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantOperationConfigRepository extends JpaRepository<TenantOperationConfigEntity, Long> {

    List<TenantOperationConfigEntity> findByTenantIdAndAppIdOrderByCreatedAtDesc(String tenantId, String appId);

    Optional<TenantOperationConfigEntity> findByTenantIdAndAppIdAndOperationType(String tenantId, String appId, String operationType);
}
