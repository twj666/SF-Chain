package com.sfchain.core.registry;

import com.sfchain.core.exception.SFChainException;
import com.sfchain.core.model.AIModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: 模型注册中心，管理所有可用的AI模型
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class ModelRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ModelRegistry.class);

    /**
     * 模型映射，键为模型名称，值为模型实例
     */
    private final Map<String, AIModel> models = new ConcurrentHashMap<>();

    /**
     * 构造函数，自动注册所有模型
     *
     * @param modelList Spring自动注入的模型列表
     */
    @Autowired
    public ModelRegistry(List<AIModel> modelList) {
        modelList.forEach(model -> {
            String modelName = model.getName();
            models.put(modelName, model);
            logger.info("Registered AI Model: {}", modelName);
        });
    }

    /**
     * 获取指定名称的模型
     *
     * @param modelName 模型名称
     * @return 模型实例
     * @throws SFChainException 如果模型未注册
     */
    public AIModel getModel(String modelName) {
        return Optional.ofNullable(models.get(modelName))
                .orElseThrow(() -> new SFChainException("Unregistered model: " + modelName));
    }

    /**
     * 获取所有注册的模型
     *
     * @return 模型映射
     */
    public Map<String, AIModel> getAllModels() {
        return Map.copyOf(models);
    }
}