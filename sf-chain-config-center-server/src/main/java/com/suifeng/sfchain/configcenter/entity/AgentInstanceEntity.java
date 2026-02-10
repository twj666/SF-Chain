package com.suifeng.sfchain.configcenter.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "sfchain_cp_agent_instances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sfchain_cp_agent", columnNames = {"tenant_id", "app_id", "instance_id"})
})
@TypeDef(name = "json", typeClass = JsonType.class)
public class AgentInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "app_id", nullable = false, length = 128)
    private String appId;

    @Column(name = "instance_id", nullable = false, length = 128)
    private String instanceId;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ONLINE";

    @Type(type = "json")
    @Column(name = "metadata_json", columnDefinition = "json")
    private Map<String, Object> metadataJson;

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (lastHeartbeatAt == null) {
            lastHeartbeatAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
