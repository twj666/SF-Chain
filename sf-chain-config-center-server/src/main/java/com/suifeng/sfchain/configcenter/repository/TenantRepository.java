package com.suifeng.sfchain.configcenter.repository;

import com.suifeng.sfchain.configcenter.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
}
