package com.suifeng.sfchain.core;

import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 描述: AI模型注册中心
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRegistry {
    
    private final OpenAIModelFactory modelFactory;
    
    /**
     * 获取模型实例
     * @param modelName 模型名称
     * @return AI模型实例
     */
    public AIModel getModel(String modelName) {
        try {
            return modelFactory.createModel(modelName);
        } catch (Exception e) {
            log.error("获取模型失败: {} - {}", modelName, e.getMessage());
            throw new RuntimeException("无法获取模型: " + modelName, e);
        }
    }
    
    /**
     * 检查模型是否已注册
     * @param modelName 模型名称
     * @return 是否已注册
     */
    public boolean isModelRegistered(String modelName) {
        return modelFactory.isModelRegistered(modelName);
    }
    
    /**
     * 获取所有已注册的模型名称
     * @return 模型名称集合
     */
    public Set<String> getRegisteredModelNames() {
        return modelFactory.getRegisteredModelNames();
    }
    
    /**
     * 获取可用的模型列表
     * @return 可用模型列表
     */
    public List<String> getAvailableModels() {
        return getRegisteredModelNames().stream()
                .filter(modelName -> {
                    try {
                        AIModel model = getModel(modelName);
                        return model.isAvailable();
                    } catch (Exception e) {
                        log.warn("检查模型可用性失败: {} - {}", modelName, e.getMessage());
                        return false;
                    }
                })
                .toList();
    }
}