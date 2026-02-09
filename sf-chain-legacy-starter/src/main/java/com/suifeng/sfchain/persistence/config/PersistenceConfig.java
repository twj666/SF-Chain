package com.suifeng.sfchain.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 持久化配置类
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Data
@Component
@ConfigurationProperties(prefix = "sf-chain.persistence")
public class PersistenceConfig {
    
    /**
     * 数据库类型，默认为MySQL
     */
    private String databaseType = "mysql";
    
    /**
     * 获取数据库类型枚举
     */
    public DatabaseType getDatabaseTypeEnum() {
        return DatabaseType.fromString(databaseType);
    }
}