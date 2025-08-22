package com.suifeng.sfchain.core.openai;

import com.suifeng.sfchain.core.AIModel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: OpenAI兼容模型工厂
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
public class OpenAIModelFactory {
    
    private final Map<String, OpenAIModelConfig> modelConfigs = new ConcurrentHashMap<>();
    private final Map<String, AIModel> modelInstances = new ConcurrentHashMap<>();
    
    /**
     * 注册模型配置
     */
    public void registerModel(OpenAIModelConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("无效的模型配置: " + config.getModelName());
        }
        
        modelConfigs.put(config.getModelName(), config);
        log.info("注册模型配置: {} ({})", config.getModelName(), config.getProvider());
    }
    
    /**
     * 创建模型实例
     */
    public AIModel createModel(String modelName) {
        return modelInstances.computeIfAbsent(modelName, name -> {
            OpenAIModelConfig config = modelConfigs.get(name);
            if (config == null) {
                throw new IllegalArgumentException("未找到模型配置: " + name);
            }
            
            if (!Boolean.TRUE.equals(config.getEnabled())) {
                throw new IllegalStateException("模型已禁用: " + name);
            }
            
            return new OpenAICompatibleModel(config);
        });
    }
    
    /**
     * 获取所有已注册的模型名称
     */
    public java.util.Set<String> getRegisteredModelNames() {
        return modelConfigs.keySet();
    }
    
    /**
     * 获取模型配置
     */
    public OpenAIModelConfig getModelConfig(String modelName) {
        return modelConfigs.get(modelName);
    }
    
    /**
     * 检查模型是否已注册
     */
    public boolean isModelRegistered(String modelName) {
        return modelConfigs.containsKey(modelName);
    }
    
    /**
     * 移除模型
     */
    public void removeModel(String modelName) {
        modelConfigs.remove(modelName);
        modelInstances.remove(modelName);
        log.info("移除模型: {}", modelName);
    }
}