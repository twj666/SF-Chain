package com.suifeng.sfchain.config;

import com.suifeng.sfchain.persistence.DatabaseInitializationService;
import com.suifeng.sfchain.persistence.DynamicOperationConfigService;
import com.suifeng.sfchain.persistence.MySQLPersistenceService;
import com.suifeng.sfchain.persistence.PersistenceManager;
import com.suifeng.sfchain.persistence.PersistenceServiceFactory;
import com.suifeng.sfchain.persistence.PostgreSQLPersistenceService;
import com.suifeng.sfchain.persistence.config.PersistenceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * SF-Chain 本地持久化自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.features.local-persistence:false}")
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(PersistenceConfig.class)
@EnableJpaRepositories(basePackages = "com.suifeng.sfchain.persistence.repository")
@EntityScan(basePackages = "com.suifeng.sfchain.persistence.entity")
@Import({
        PersistenceServiceFactory.class,
        DynamicOperationConfigService.class,
        PersistenceManager.class,
        MySQLPersistenceService.class,
        PostgreSQLPersistenceService.class
})
public class SfChainPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "sf-chain.features", name = "local-migration", havingValue = "true")
    @ConditionalOnMissingBean
    public DatabaseInitializationService databaseInitializationService(
            PersistenceConfig persistenceConfig,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate) {
        log.info("初始化SF-Chain 数据库初始化服务");
        return new DatabaseInitializationService(persistenceConfig, dataSource, jdbcTemplate);
    }
}
