package com.suifeng.sfchain.config;

import com.suifeng.sfchain.controller.IndexController;
import com.suifeng.sfchain.controller.RootController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SF-Chain 静态前端自动配置
 */
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.features.static-ui:false}")
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(SfChainPathProperties.class)
@Import({
        WebConfig.class,
        IndexController.class,
        RootController.class
})
public class SfChainStaticUiAutoConfiguration {
}
