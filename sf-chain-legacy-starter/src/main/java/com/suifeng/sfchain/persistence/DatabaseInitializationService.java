package com.suifeng.sfchain.persistence;

import com.suifeng.sfchain.persistence.config.DatabaseType;
import com.suifeng.sfchain.persistence.config.PersistenceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库初始化服务
 * 根据配置的数据库类型自动执行对应的migration脚本
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sf-chain.persistence", name = "database-type")
public class DatabaseInitializationService {
    
    private final PersistenceConfig persistenceConfig;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 应用启动后自动执行数据库初始化
     */
    @PostConstruct
    @Transactional
    public void initializeDatabase() {
        try {
            DatabaseType databaseType = persistenceConfig.getDatabaseTypeEnum();
            log.info("检测到数据库类型配置: {}，开始执行数据库初始化", databaseType);
            
            // 检查是否需要初始化
            if (shouldInitialize(databaseType)) {
                executeMigrationScript(databaseType);
                log.info("数据库初始化完成: {}", databaseType);
            } else {
                log.info("数据库已存在相关表，跳过初始化: {}", databaseType);
            }
            
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
    
    /**
     * 检查是否需要执行数据库初始化
     * 通过检查核心表是否存在来判断
     */
    private boolean shouldInitialize(DatabaseType databaseType) {
        try {
            String checkTableSql;
            switch (databaseType) {
                case MYSQL:
                    checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                  "WHERE table_schema = DATABASE() AND table_name = 'sfchain_model_configs'";
                    break;
                case POSTGRESQL:
                    checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                  "WHERE table_name = 'sfchain_model_configs' AND table_schema = 'public'";
                    break;
                default:
                    log.warn("不支持的数据库类型: {}", databaseType);
                    return false;
            }
            
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class);
            return count == null || count == 0;
            
        } catch (Exception e) {
            log.debug("检查表存在性时出现异常，假设需要初始化: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 执行对应数据库类型的migration脚本
     */
    private void executeMigrationScript(DatabaseType databaseType) throws SQLException {
        String scriptPath = getMigrationScriptPath(databaseType);
        log.info("执行数据库脚本: {}", scriptPath);
        
        try (Connection connection = dataSource.getConnection()) {
            ClassPathResource resource = new ClassPathResource(scriptPath);
            if (!resource.exists()) {
                throw new RuntimeException("Migration脚本不存在: " + scriptPath);
            }
            
            ScriptUtils.executeSqlScript(connection, resource);
            log.info("成功执行数据库脚本: {}", scriptPath);
            
        } catch (Exception e) {
            log.error("执行数据库脚本失败: {}", scriptPath, e);
            throw new SQLException("执行数据库脚本失败: " + scriptPath, e);
        }
    }
    
    /**
     * 根据数据库类型获取对应的migration脚本路径
     */
    private String getMigrationScriptPath(DatabaseType databaseType) {
        switch (databaseType) {
            case MYSQL:
                return "migration/v1_mysql.sql";
            case POSTGRESQL:
                return "migration/v1_postgresql.sql";
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
    }
}