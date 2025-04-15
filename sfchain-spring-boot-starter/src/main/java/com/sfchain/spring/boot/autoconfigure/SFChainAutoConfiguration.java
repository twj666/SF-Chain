package com.sfchain.spring.boot.autoconfigure;

import com.sfchain.spring.boot.properties.SFChainProperties;
import com.sfchain.spring.config.SFChainConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * SFChain自动配置类
 * 
 * @author SuiFeng
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties(SFChainProperties.class)
@Import(SFChainConfiguration.class)
public class SFChainAutoConfiguration {
    
    /**
     * 创建配置Bean
     * 
     * @param properties 配置属性
     * @return 配置Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public SFChainConfiguration sfChainConfiguration(SFChainProperties properties) {
        return new SFChainConfiguration();
    }
}