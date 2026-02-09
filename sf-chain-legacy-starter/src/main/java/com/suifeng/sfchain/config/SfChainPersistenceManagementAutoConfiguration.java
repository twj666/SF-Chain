package com.suifeng.sfchain.config;

import com.suifeng.sfchain.controller.AIModelController;
import com.suifeng.sfchain.controller.AIOperationController;
import com.suifeng.sfchain.controller.AISystemController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SF-Chain 依赖本地持久化的管理API自动配置
 */
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.features.management-api:false} and ${sf-chain.features.local-persistence:false}")
@ConditionalOnClass(WebMvcConfigurer.class)
@Import({
        AIModelController.class,
        AIOperationController.class,
        AISystemController.class
})
public class SfChainPersistenceManagementAutoConfiguration {
}
