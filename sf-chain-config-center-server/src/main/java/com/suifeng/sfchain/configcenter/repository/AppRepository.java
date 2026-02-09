package com.suifeng.sfchain.configcenter.repository;

import com.suifeng.sfchain.configcenter.entity.AppEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppRepository extends JpaRepository<AppEntity, Long> {

    List<AppEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<AppEntity> findByTenantIdAndAppId(String tenantId, String appId);

    boolean existsByTenantIdAndAppId(String tenantId, String appId);
}
