package com.suifeng.sfchain.configcenter.config;

import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SfChainCoreBridgeConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAIModelFactory openAIModelFactory(
            @Value("${sf-chain.openai.connect-timeout-ms:30000}") int connectTimeoutMs,
            @Value("${sf-chain.openai.read-timeout-ms:300000}") int readTimeoutMs) {
        return new OpenAIModelFactory(connectTimeoutMs, readTimeoutMs);
    }
}
