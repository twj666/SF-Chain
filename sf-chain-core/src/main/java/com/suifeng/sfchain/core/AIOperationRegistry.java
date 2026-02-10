package com.suifeng.sfchain.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: AI操作注册中心 - 新框架版本
 * 管理AI操作和模型的映射关系
 * 
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "ai.operations")
public class AIOperationRegistry {
    
    /**
     * 操作到实例的映射
     */
    private final Map<String, BaseAIOperation<?, ?>> operationMap = new ConcurrentHashMap<>();
    
    /**
     * 操作到模型的映射配置
     * -- GETTER --
     *  获取模型映射配置（用于配置文件绑定）
     * -- SETTER --
     *  设置模型映射配置（用于配置文件绑定）
     */
    @Setter
    @Getter
    private Map<String, String> modelMapping = new ConcurrentHashMap<>();
    
    /**
     * 操作的默认配置
     * -- GETTER --
     *  获取操作配置（用于配置文件绑定）
     * -- SETTER --
     *  设置操作配置（用于配置文件绑定）
     */
    @Setter
    @Getter
    private Map<String, OperationConfig> configs = new ConcurrentHashMap<>();
    
    @Resource
    private ModelRegistry modelRegistry;

    /**
     * 注册操作
     * 
     * @param operationType 操作类型
     * @param operation 操作实例
     */
    public void registerOperation(String operationType, BaseAIOperation<?, ?> operation) {
        operationMap.put(operationType, operation);
        log.info("注册AI操作: {} -> {}", operationType, operation.getClass().getSimpleName());
    }
    
    /**
     * 获取操作实例
     * 
     * @param operationType 操作类型
     * @return 操作实例
     */
    public BaseAIOperation<?, ?> getOperation(String operationType) {
        BaseAIOperation<?, ?> operation = operationMap.get(operationType);
        if (operation == null) {
            throw new IllegalArgumentException("未找到操作: " + operationType);
        }
        return operation;
    }
    
    /**
     * 获取操作对应的模型
     * 
     * @param operationType 操作类型
     * @return 模型名称
     */
    public String getModelForOperation(String operationType) {
        // 优先从内存缓存获取
        return modelMapping.get(operationType);
    }
    
    /**
     * 设置操作的模型映射
     * 
     * @param operationType 操作类型
     * @param modelName 模型名称
     */
    public void setModelForOperation(String operationType, String modelName) {
        // 验证模型是否存在
        AIModel model = modelRegistry.getModel(modelName);
        if (model == null) {
            throw new IllegalArgumentException("模型不存在: " + modelName);
        }
        
        modelMapping.put(operationType, modelName);
        log.info("设置操作模型映射: {} -> {}", operationType, modelName);
    }
    
    /**
     * 获取操作配置
     * 
     * @param operationType 操作类型
     * @return 操作配置
     */
    public OperationConfig getOperationConfig(String operationType) {
        return configs.getOrDefault(operationType, new OperationConfig());
    }
    
    /**
     * 获取所有已注册的操作
     * 
     * @return 操作类型列表
     */
    public List<String> getAllOperations() {
        return List.copyOf(operationMap.keySet());
    }
    
    /**
     * 检查操作是否已注册
     * 
     * @param operationType 操作类型
     * @return 是否已注册
     */
    public boolean isOperationRegistered(String operationType) {
        return operationMap.containsKey(operationType);
    }

    /**
     * 操作配置类
     */
    @Setter
    @Getter
    public static class OperationConfig {
        private boolean enabled = true;
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private boolean requireJsonOutput = true;
        private boolean supportThinking = false;
        private int timeoutSeconds = 30;
        private int retryCount = 2;
    }
}