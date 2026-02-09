package com.suifeng.sfchain.config;

import com.suifeng.sfchain.controller.AICallLogController;
import com.suifeng.sfchain.controller.SfChainConfigController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SF-Chain 管理API自动配置（无本地持久化依赖）
 */
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.features.management-api:false}")
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(SfChainPathProperties.class)
@Import({
        AuthorizationInterceptor.class,
        SfChainWebConfig.class,
        SfChainConfigController.class,
        AICallLogController.class
})
public class SfChainManagementAutoConfiguration {
}
