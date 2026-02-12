package com.suifeng.sfchain.config;

import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.AIService;
import com.suifeng.sfchain.core.ModelRegistry;
import com.suifeng.sfchain.core.PromptTemplateEngine;
import com.suifeng.sfchain.core.logging.AICallLogAspect;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadGateway;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import com.suifeng.sfchain.operations.JSONRepairOperation;
import com.suifeng.sfchain.operations.ModelValidationOperation;
import com.suifeng.sfchain.persistence.context.ChatContextService;
import com.suifeng.sfchain.persistence.context.MapBasedChatContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SF-Chain 自动配置类
 * 仅负责核心执行能力装配
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        SfChainFeaturesProperties.class,
        SfChainLoggingProperties.class
})
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
    public AICallLogManager aiCallLogManager(
            SfChainLoggingProperties logProperties,
            ObjectProvider<AICallLogUploadGateway> uploadGatewayProvider) {
        log.info("初始化SF-Chain AI调用日志管理器");
        return new AICallLogManager(logProperties, uploadGatewayProvider.getIfAvailable(() -> AICallLogUploadGateway.NO_OP));
    }

    @Bean
    @ConditionalOnMissingBean
    public AICallLogAspect aiCallLogAspect(AICallLogManager logManager) {
        log.info("初始化SF-Chain AI调用日志切面");
        return new AICallLogAspect(logManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptTemplateEngine promptTemplateEngine(ObjectMapper objectMapper) {
        return new PromptTemplateEngine(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JSONRepairOperation jsonRepairOperation() {
        return new JSONRepairOperation();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelValidationOperation modelValidationOperation() {
        return new ModelValidationOperation();
    }
}
