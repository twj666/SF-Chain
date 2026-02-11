package com.suifeng.sfchain.configcenter.repository;

import com.suifeng.sfchain.configcenter.entity.AgentInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentInstanceRepository extends JpaRepository<AgentInstanceEntity, Long> {

    Optional<AgentInstanceEntity> findByTenantIdAndAppIdAndInstanceId(String tenantId, String appId, String instanceId);

    @Query("select ai.tenantId as tenantId, ai.appId as appId, max(ai.lastHeartbeatAt) as lastHeartbeatAt, " +
            "sum(case when ai.lastHeartbeatAt >= :onlineCutoff then 1 else 0 end) as instanceCount " +
            "from AgentInstanceEntity ai group by ai.tenantId, ai.appId")
    List<OnlineHeartbeatProjection> findLatestHeartbeatsByApp(@Param("onlineCutoff") LocalDateTime onlineCutoff);

    void deleteByLastHeartbeatAtBefore(LocalDateTime cutoff);

    interface OnlineHeartbeatProjection {
        String getTenantId();
        String getAppId();
        LocalDateTime getLastHeartbeatAt();
        long getInstanceCount();
    }
}
