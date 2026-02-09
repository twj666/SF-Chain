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
@Table(name = "sfchain_cp_model_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sfchain_cp_model", columnNames = {"tenant_id", "app_id", "model_name"})
})
@TypeDef(name = "json", typeClass = JsonType.class)
public class TenantModelConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "app_id", nullable = false, length = 128)
    private String appId;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    @Type(type = "json")
    @Column(name = "config_json", columnDefinition = "json")
    private Map<String, Object> configJson;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
