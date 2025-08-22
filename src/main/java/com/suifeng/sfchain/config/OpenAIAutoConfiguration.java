package com.suifeng.sfchain.config;

import com.suifeng.sfchain.core.AIModel;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述: OpenAI兼容模型自动配
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OpenAIModelsConfig.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.openai-models", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenAIAutoConfiguration {
    
    private final OpenAIModelsConfig openAIModelsConfig;
    
    /**
     * 创建OpenAI模型工厂
     */
    @Bean
    @Primary
    public OpenAIModelFactory openAIModelFactory() {
        OpenAIModelFactory factory = new OpenAIModelFactory();
        
        // 注册配置文件中的模型
        Map<String, OpenAIModelConfig> modelConfigs = openAIModelsConfig.getValidModelConfigs();
        modelConfigs.forEach((name, config) -> {
            try {
                factory.registerModel(config);
                log.info("成功注册模型: {} ({})", config.getModelName(), config.getProvider());
            } catch (Exception e) {
                log.error("注册模型失败: {} - {}", config.getModelName(), e.getMessage());
            }
        });
        
        return factory;
    }
    
    /**
     * 创建AI模型列表，供ModelRegistry使用
     */
    @Bean
    public List<AIModel> aiModels(OpenAIModelFactory factory) {
        List<AIModel> models = new ArrayList<>();
        
        // 为每个注册的模型创建实例
        factory.getRegisteredModelNames().forEach(modelName -> {
            try {
                AIModel model = factory.createModel(modelName);
                models.add(model);
                log.info("创建AI模型实例: {}", modelName);
            } catch (Exception e) {
                log.error("创建模型实例失败: {} - {}", modelName, e.getMessage());
            }
        });
        
        return models;
    }
}