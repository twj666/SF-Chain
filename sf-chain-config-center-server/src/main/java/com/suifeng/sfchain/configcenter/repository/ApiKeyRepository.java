package com.suifeng.sfchain.configcenter.repository;

import com.suifeng.sfchain.configcenter.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {

    List<ApiKeyEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<ApiKeyEntity> findByTenantIdAndAppIdOrderByCreatedAtDesc(String tenantId, String appId);

    List<ApiKeyEntity> findByKeyPrefix(String keyPrefix);
}
