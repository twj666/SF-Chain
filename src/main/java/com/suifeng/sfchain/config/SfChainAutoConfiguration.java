package com.suifeng.sfchain.config;

import com.suifeng.sfchain.core.*;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import com.suifeng.sfchain.persistence.PersistenceManager;
import com.suifeng.sfchain.persistence.PersistenceServiceFactory;
import com.suifeng.sfchain.persistence.DynamicOperationConfigService;
import com.suifeng.sfchain.persistence.DatabaseInitializationService;
import com.suifeng.sfchain.persistence.context.ChatContextService;
import com.suifeng.sfchain.persistence.context.MapBasedChatContextService;
import com.suifeng.sfchain.persistence.config.PersistenceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * SF-Chain 自动配置类
 * 提供框架的自动装配功能
 * 
 * @author suifeng
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = "com.suifeng.sfchain")
@EnableJpaRepositories(basePackages = "com.suifeng.sfchain.persistence.repository")
@EntityScan(basePackages = "com.suifeng.sfchain.persistence.entity")
@EnableConfigurationProperties(PersistenceConfig.class)
@ConditionalOnProperty(prefix = "sf-chain", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SfChainAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public AIOperationRegistry aiOperationRegistry() {
        log.info("初始化SF-Chain AI操作注册表");
        return new AIOperationRegistry();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRegistry modelRegistry(OpenAIModelFactory openAIModelFactory) {
        log.info("初始化SF-Chain 模型注册表");
        return new ModelRegistry(openAIModelFactory);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AIService aiService() {
        log.info("初始化SF-Chain AI服务");
        return new AIService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ChatContextService chatContextService() {
        log.info("初始化SF-Chain 聊天上下文服务");
        return new MapBasedChatContextService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PersistenceManager persistenceManager(
            PersistenceServiceFactory persistenceServiceFactory,
            ModelRegistry modelRegistry,
            AIOperationRegistry operationRegistry,
            OpenAIModelFactory modelFactory,
            DynamicOperationConfigService dynamicOperationConfigService) {
        log.info("初始化SF-Chain 持久化管理器");
        return new PersistenceManager(
                persistenceServiceFactory,
                modelRegistry,
                operationRegistry,
                modelFactory,
                dynamicOperationConfigService
        );
    }
    
    /**
     * 数据库初始化服务
     * 只有在配置了数据库类型时才会创建
     */
    @Bean
    @ConditionalOnProperty(prefix = "sf-chain.persistence", name = "database-type")
    @ConditionalOnMissingBean
    public DatabaseInitializationService databaseInitializationService(
            PersistenceConfig persistenceConfig,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate) {
        log.info("初始化SF-Chain 数据库初始化服务");
        return new DatabaseInitializationService(persistenceConfig, dataSource, jdbcTemplate);
    }
}