package com.suifeng.sfchain.persistence;

import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.BaseAIOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 描述: 动态操作配置服务
 * 从@AIOp注解中获取操作配置信息
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicOperationConfigService {
    
    private final AIOperationRegistry operationRegistry;
    
    /**
     * 从@AIOp注解获取操作配置
     * 
     * @param operationType 操作类型
     * @return 操作配置
     */
    public Optional<OperationConfigData> getOperationConfig(String operationType) {
        try {
            if (!operationRegistry.isOperationRegistered(operationType)) {
                return Optional.empty();
            }
            
            BaseAIOperation<?, ?> operation = operationRegistry.getOperation(operationType);
            AIOp annotation = operation.getAnnotation();
            
            if (annotation == null) {
                return Optional.empty();
            }
            
            // 从注解构建配置
            OperationConfigData config = OperationConfigData.builder()
                    .operationType(operationType)
                    .description(annotation.description())
                    .enabled(annotation.enabled())
                    .maxTokens(annotation.defaultMaxTokens() > 0 ? annotation.defaultMaxTokens() : null)
                    .temperature(annotation.defaultTemperature() >= 0 ? annotation.defaultTemperature() : null)
                    .jsonOutput(annotation.requireJsonOutput())
                    .thinkingMode(annotation.supportThinking())
                    .modelName(annotation.defaultModel().isEmpty() ? null : annotation.defaultModel())
                    .build();
            
            return Optional.of(config);
        } catch (Exception e) {
            log.warn("获取操作 {} 的动态配置失败: {}", operationType, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有操作的动态配置
     * 
     * @return 所有动态配置
     */
    public Map<String, OperationConfigData> getAllOperationConfigs() {
        Map<String, OperationConfigData> configs = new HashMap<>();
        
        for (String operationType : operationRegistry.getAllOperations()) {
            getOperationConfig(operationType).ifPresent(config -> 
                configs.put(operationType, config)
            );
        }
        
        return configs;
    }
}