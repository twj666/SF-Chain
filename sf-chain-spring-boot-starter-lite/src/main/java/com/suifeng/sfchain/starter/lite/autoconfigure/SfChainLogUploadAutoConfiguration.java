package com.suifeng.sfchain.starter.lite.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadClient;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadGateway;
import com.suifeng.sfchain.core.logging.upload.AsyncAICallLogUploader;
import com.suifeng.sfchain.starter.lite.logging.upload.HttpAICallLogUploadClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * SF-Chain 日志异步上报自动配置
 */
@AutoConfiguration
@ConditionalOnExpression("${sf-chain.enabled:true} and ${sf-chain.logging.upload-enabled:false}")
@EnableConfigurationProperties({
        SfChainLoggingProperties.class,
        SfChainServerProperties.class
})
public class SfChainLogUploadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AICallLogUploadClient aiCallLogUploadClient(
            ObjectMapper objectMapper,
            SfChainServerProperties serverProperties,
            SfChainLoggingProperties loggingProperties) {
        return new HttpAICallLogUploadClient(objectMapper, serverProperties, loggingProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AICallLogUploadGateway aiCallLogUploadGateway(
            SfChainLoggingProperties loggingProperties,
            AICallLogUploadClient uploadClient) {
        return new AsyncAICallLogUploader(loggingProperties, uploadClient);
    }
}
