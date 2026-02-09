package com.suifeng.sfchain.persistence;

import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.ModelRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class PersistenceManager {
    
    private final PersistenceService persistenceService;
    private final ModelRegistry modelRegistry;
    private final AIOperationRegistry operationRegistry;
    private final OpenAIModelFactory modelFactory;
    private final DynamicOperationConfigService dynamicOperationConfigService;
    
    // 统一使用构造函数注入
    public PersistenceManager(
            PersistenceServiceFactory persistenceServiceFactory,
            ModelRegistry modelRegistry,
            AIOperationRegistry operationRegistry,
            OpenAIModelFactory modelFactory,
            DynamicOperationConfigService dynamicOperationConfigService) {
        this.persistenceService = persistenceServiceFactory.createPersistenceService();
        this.modelRegistry = modelRegistry;
        this.operationRegistry = operationRegistry;
        this.modelFactory = modelFactory;
        this.dynamicOperationConfigService = dynamicOperationConfigService;
    }
    
    /**
     * 应用启动完成后同步配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始同步持久化配置...");
        
        // 同步现有配置到持久化存储
        syncExistingConfigurations();
        
        // 从持久化存储加载额外配置
        loadPersistedConfigurations();
        
        log.info("持久化配置同步完成");
    }
    
    // ==================== 模型配置管理 ====================
    
    /**
     * 添加模型配置
     * 
     * @param modelName 模型名称
     * @param config 模型配置数据
     */
    public void addModelConfig(String modelName, ModelConfigData config) {
        try {
            // 验证配置
            if (!config.isValid()) {
                throw new IllegalArgumentException("无效的模型配置: " + modelName);
            }
            
            // 转换为OpenAI模型配置
            OpenAIModelConfig openAIConfig = convertToOpenAIConfig(config);
            
            // 注册到模型工厂
            modelFactory.registerModel(openAIConfig);
            
            // 保存到持久化存储
            persistenceService.saveModelConfig(modelName, config);
            
            log.info("成功添加模型配置: {}", modelName);
        } catch (Exception e) {
            log.error("添加模型配置失败: {} - {}", modelName, e.getMessage());
            throw new RuntimeException("添加模型配置失败: " + modelName, e);
        }
    }
    
    /**
     * 更新模型配置
     * 
     * @param modelName 模型名称
     * @param config 模型配置数据
     */
    public void updateModelConfig(String modelName, ModelConfigData config) {
        try {
            // 检查模型是否存在
            if (!persistenceService.existsModelConfig(modelName)) {
                throw new IllegalArgumentException("模型配置不存在: " + modelName);
            }
            
            // 验证配置
            if (!config.isValid()) {
                throw new IllegalArgumentException("无效的模型配置: " + modelName);
            }
            
            // 移除旧配置
            modelFactory.removeModel(modelName);
            
            // 转换为OpenAI模型配置
            OpenAIModelConfig openAIConfig = convertToOpenAIConfig(config);
            
            // 重新注册到模型工厂
            modelFactory.registerModel(openAIConfig);
            
            // 更新持久化存储
            persistenceService.saveModelConfig(modelName, config);
            
            log.info("成功更新模型配置: {}", modelName);
        } catch (Exception e) {
            log.error("更新模型配置失败: {} - {}", modelName, e.getMessage());
            throw new RuntimeException("更新模型配置失败: " + modelName, e);
        }
    }
    
    /**
     * 删除模型配置
     * 
     * @param modelName 模型名称
     */
    public void deleteModelConfig(String modelName) {
        try {
            // 检查是否有操作正在使用此模型（从操作配置中检查）
            Map<String, OperationConfigData> allConfigs = persistenceService.getAllOperationConfigs();
            boolean inUse = allConfigs.values().stream()
                    .anyMatch(config -> modelName.equals(config.getModelName()));
            if (inUse) {
                throw new IllegalStateException("模型正在被操作使用，无法删除: " + modelName);
            }
            
            // 从模型工厂移除
            modelFactory.removeModel(modelName);
            
            // 从持久化存储删除
            persistenceService.deleteModelConfig(modelName);
            
            log.info("成功删除模型配置: {}", modelName);
        } catch (Exception e) {
            log.error("删除模型配置失败: {} - {}", modelName, e.getMessage());
            throw new RuntimeException("删除模型配置失败: " + modelName, e);
        }
    }
    
    /**
     * 获取模型配置
     * 
     * @param modelName 模型名称
     * @return 模型配置
     */
    public Optional<ModelConfigData> getModelConfig(String modelName) {
        return persistenceService.getModelConfig(modelName);
    }
    
    /**
     * 获取所有模型配置
     * 
     * @return 所有模型配置
     */
    public Map<String, ModelConfigData> getAllModelConfigs() {
        return persistenceService.getAllModelConfigs();
    }
    
    // ==================== 操作配置管理 ====================
    
    /**
     * 保存操作配置
     * 同时更新数据库和内存容器
     * 
     * @param operationType 操作类型
     * @param config 操作配置
     */
    public void saveOperationConfig(String operationType, OperationConfigData config) {
        try {
            // 验证操作是否已注册
            if (!operationRegistry.isOperationRegistered(operationType)) {
                throw new IllegalArgumentException("操作未注册: " + operationType);
            }
            
            // 验证配置
            if (!config.isValid()) {
                throw new IllegalArgumentException("无效的操作配置: " + operationType);
            }
            
            // 如果配置中指定了模型，验证模型是否存在
            if (config.getModelName() != null && !config.getModelName().isEmpty()) {
                if (!persistenceService.existsModelConfig(config.getModelName()) && 
                    !modelRegistry.isModelRegistered(config.getModelName())) {
                    throw new IllegalArgumentException("指定的模型不存在: " + config.getModelName());
                }
            }
            
            // 1. 先保存到数据库
            persistenceService.saveOperationConfig(operationType, config);
            
            // 2. ✅ 完整同步所有配置到框架（包括模型映射和其他参数）
            syncDatabaseConfigToFramework(operationType, config);
            
            log.info("成功保存操作配置并完整同步到框架: {} -> 模型: {}", operationType, config.getModelName());
        } catch (Exception e) {
            log.error("保存操作配置失败: {} - {}", operationType, e.getMessage());
            throw new RuntimeException("保存操作配置失败: " + operationType, e);
        }
    }
    
    /**
     * 获取操作配置
     * 优先从数据库获取，如果不存在则从@AIOp注解获取
     * 
     * @param operationType 操作类型
     * @return 操作配置
     */
    public Optional<OperationConfigData> getOperationConfig(String operationType) {
        // 优先从数据库获取
        Optional<OperationConfigData> persistedConfig = persistenceService.getOperationConfig(operationType);
        if (persistedConfig.isPresent()) {
            log.debug("从数据库获取操作配置: {}", operationType);
            return persistedConfig;
        }
        
        // 如果数据库配置不存在，则从注解获取
        Optional<OperationConfigData> dynamicConfig = dynamicOperationConfigService.getOperationConfig(operationType);
        if (dynamicConfig.isPresent()) {
            log.debug("从注解获取操作配置: {}", operationType);
        }
        
        return dynamicConfig;
    }
    
    /**
     * 获取所有操作配置
     * 优先使用数据库配置，注解配置作为补充
     * 
     * @return 所有操作配置
     */
    public Map<String, OperationConfigData> getAllOperationConfigs() {
        // 获取持久化配置（数据库）
        Map<String, OperationConfigData> persistedConfigs = persistenceService.getAllOperationConfigs();
        
        // 获取动态配置（从注解）
        Map<String, OperationConfigData> dynamicConfigs = dynamicOperationConfigService.getAllOperationConfigs();
        
        // 合并配置，数据库配置优先
        Map<String, OperationConfigData> allConfigs = new HashMap<>(dynamicConfigs);
        allConfigs.putAll(persistedConfigs); // 数据库配置覆盖注解配置
        
        log.debug("获取所有操作配置: 持久化配置{}个, 动态配置{}个, 总计{}个", 
                persistedConfigs.size(), dynamicConfigs.size(), allConfigs.size());
        
        return allConfigs;
    }
    
    /**
     * 删除操作配置
     * 
     * @param operationType 操作类型
     */
    public void deleteOperationConfig(String operationType) {
        try {
            persistenceService.deleteOperationConfig(operationType);
            
            // 同时从操作注册中心移除模型映射
            operationRegistry.getModelMapping().remove(operationType);
            
            log.info("成功删除操作配置: {}", operationType);
        } catch (Exception e) {
            log.error("删除操作配置失败: {} - {}", operationType, e.getMessage());
            throw new RuntimeException("删除操作配置失败: " + operationType, e);
        }
    }
    
    // ==================== 备份和恢复 ====================
    
    /**
     * 创建配置备份
     * 
     * @param backupName 备份名称
     */
    public void createBackup(String backupName) {
        persistenceService.backup(backupName);
    }
    
    /**
     * 从备份恢复配置
     * 
     * @param backupName 备份名称
     */
    public void restoreFromBackup(String backupName) {
        try {
            // 恢复配置
            persistenceService.restoreFromBackup(backupName);
            
            // 重新同步配置
            loadPersistedConfigurations();
            
            log.info("成功从备份恢复配置: {}", backupName);
        } catch (Exception e) {
            log.error("从备份恢复配置失败: {} - {}", backupName, e.getMessage());
            throw new RuntimeException("从备份恢复配置失败: " + backupName, e);
        }
    }
    
    /**
     * 获取所有备份名称
     * 
     * @return 备份名称列表
     */
    public List<String> getAllBackupNames() {
        return persistenceService.getAllBackupNames();
    }
    
    /**
     * 刷新配置到持久化存储
     */
    public void flushConfigurations() {
        syncExistingConfigurations();
        log.info("配置已刷新到持久化存储");
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfigurations() {
        try {
            // 重新加载配置
            loadPersistedConfigurations();
            
            log.info("配置重新加载完成");
        } catch (Exception e) {
            log.error("重新加载配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("重新加载配置失败", e);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 同步现有配置到持久化存储
     */
    private void syncExistingConfigurations() {
        try {
            // 同步现有的模型配置
            for (String modelName : modelFactory.getRegisteredModelNames()) {
                OpenAIModelConfig openAIConfig = modelFactory.getModelConfig(modelName);
                if (openAIConfig != null && !persistenceService.existsModelConfig(modelName)) {
                    ModelConfigData configData = convertFromOpenAIConfig(openAIConfig);
                    persistenceService.saveModelConfig(modelName, configData);
                }
            }
            
            // 初始化操作配置：从@AIOp注解获取配置并保存到数据库
            initializeOperationConfigs();
            
        } catch (Exception e) {
            log.warn("同步现有配置时出现警告: {}", e.getMessage());
        }
    }
    
    /**
     * 从持久化存储加载配置
     */
    private void loadPersistedConfigurations() {
        try {
            // 加载模型配置
            Map<String, ModelConfigData> modelConfigs = persistenceService.getAllModelConfigs();
            for (Map.Entry<String, ModelConfigData> entry : modelConfigs.entrySet()) {
                String modelName = entry.getKey();
                ModelConfigData config = entry.getValue();
                
                if (!modelFactory.isModelRegistered(modelName)) {
                    try {
                        OpenAIModelConfig openAIConfig = convertToOpenAIConfig(config);
                        modelFactory.registerModel(openAIConfig);
                        log.info("从持久化存储加载模型配置: {}", modelName);
                    } catch (Exception e) {
                        log.warn("加载模型配置失败: {} - {}", modelName, e.getMessage());
                    }
                }
            }
            
            // 加载操作配置并同步模型映射到操作注册中心
            Map<String, OperationConfigData> operationConfigs = persistenceService.getAllOperationConfigs();
            for (Map.Entry<String, OperationConfigData> entry : operationConfigs.entrySet()) {
                String operationType = entry.getKey();
                OperationConfigData config = entry.getValue();
                
                // 如果操作配置中指定了模型，同步到操作注册中心
                if (config.getModelName() != null && !config.getModelName().isEmpty() && 
                    operationRegistry.isOperationRegistered(operationType)) {
                    try {
                        operationRegistry.setModelForOperation(operationType, config.getModelName());
                        log.debug("从持久化存储同步操作模型映射: {} -> {}", operationType, config.getModelName());
                    } catch (Exception e) {
                        log.warn("同步操作模型映射失败: {} -> {} - {}", operationType, config.getModelName(), e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("加载持久化配置失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 初始化操作配置
     * 1. 从数据库获取所有操作配置
     * 2. 与框架中的操作进行对比
     * 3. 数据库没有的操作新增到数据库
     * 4. 数据库有的操作以数据库配置为准同步到框架
     */
    private void initializeOperationConfigs() {
        try {
            // 1. 获取数据库中所有的操作配置
            Map<String, OperationConfigData> dbConfigs = persistenceService.getAllOperationConfigs();
            
            // 2. 获取框架中所有已注册的操作
            List<String> frameworkOperations = operationRegistry.getAllOperations();
            
            // 3. 处理框架中的操作
            for (String operationType : frameworkOperations) {
                if (dbConfigs.containsKey(operationType)) {
                    // 数据库中存在该操作配置，以数据库配置为准同步到框架
                    OperationConfigData dbConfig = dbConfigs.get(operationType);
                    syncDatabaseConfigToFramework(operationType, dbConfig);
                    log.info("使用数据库配置同步到框架: {} -> 模型: {}", operationType, dbConfig.getModelName());
                } else {
                    // 数据库中不存在该操作配置，从注解获取并新增到数据库
                    Optional<OperationConfigData> annotationConfig = dynamicOperationConfigService.getOperationConfig(operationType);
                    if (annotationConfig.isPresent()) {
                        OperationConfigData config = annotationConfig.get();
                        persistenceService.saveOperationConfig(operationType, config);
                        syncDatabaseConfigToFramework(operationType, config);
                        log.info("从注解新增操作配置到数据库: {} -> 模型: {}", operationType, config.getModelName());
                    } else {
                        log.debug("操作 {} 没有@AIOp注解配置，跳过初始化", operationType);
                    }
                }
            }
            
            // 4. 处理数据库中存在但框架中不存在的操作（可能是已删除的操作）
            for (String dbOperationType : dbConfigs.keySet()) {
                if (!frameworkOperations.contains(dbOperationType)) {
                    log.warn("数据库中存在操作配置但框架中未注册该操作: {}，建议清理数据库配置", dbOperationType);
                    // 可选：自动删除数据库中的无效配置
                     persistenceService.deleteOperationConfig(dbOperationType);
                }
            }
            
        } catch (Exception e) {
            log.error("初始化操作配置时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将数据库配置同步到框架中
     * 
     * @param operationType 操作类型
     * @param dbConfig 数据库配置
     */
    private void syncDatabaseConfigToFramework(String operationType, OperationConfigData dbConfig) {
        try {
            log.debug("同步数据库配置到框架: {} -> {}", operationType, dbConfig);
            
            // 1. 更新AIOperationRegistry中的操作配置
            updateOperationConfigInRegistry(operationType, dbConfig);
            
            // 2. 同步模型映射关系
            if (dbConfig.getModelName() != null && !dbConfig.getModelName().trim().isEmpty()) {
                syncModelMapping(operationType, dbConfig.getModelName());
            }
            
            log.info("成功同步操作配置到框架: {}", operationType);
            
        } catch (Exception e) {
            log.error("同步操作配置到框架失败: {} - {}", operationType, e.getMessage(), e);
            throw new RuntimeException("同步配置失败: " + operationType, e);
        }
    }
    
    /**
     * 更新AIOperationRegistry中的操作配置
     * 
     * @param operationType 操作类型
     * @param dbConfig 数据库配置
     */
    private void updateOperationConfigInRegistry(String operationType, OperationConfigData dbConfig) {
        // 获取或创建操作配置
        AIOperationRegistry.OperationConfig registryConfig = 
            operationRegistry.getOperationConfig(operationType);
        
        if (registryConfig == null) {
            registryConfig = new AIOperationRegistry.OperationConfig();
        }
        
        // 将数据库配置映射到注册中心配置
        if (dbConfig.getEnabled() != null) {
            registryConfig.setEnabled(dbConfig.getEnabled());
        }
        
        if (dbConfig.getMaxTokens() != null && dbConfig.getMaxTokens() > 0) {
            registryConfig.setMaxTokens(dbConfig.getMaxTokens());
        }
        
        if (dbConfig.getTemperature() != null && dbConfig.getTemperature() >= 0) {
            registryConfig.setTemperature(dbConfig.getTemperature());
        }
        
        if (dbConfig.getJsonOutput() != null) {
            registryConfig.setRequireJsonOutput(dbConfig.getJsonOutput());
        }
        
        if (dbConfig.getThinkingMode() != null) {
            registryConfig.setSupportThinking(dbConfig.getThinkingMode());
        }
        
        if (dbConfig.getRetryCount() != null && dbConfig.getRetryCount() >= 0) {
            registryConfig.setRetryCount(dbConfig.getRetryCount());
        }
        
        // 处理超时时间转换（数据库存储毫秒，注册中心使用秒）
        if (dbConfig.getTimeout() != null && dbConfig.getTimeout() > 0) {
            int timeoutSeconds = (int) (dbConfig.getTimeout() / 1000);
            registryConfig.setTimeoutSeconds(Math.max(1, timeoutSeconds));
        }
        
        // 将更新后的配置设置回注册中心
        operationRegistry.getConfigs().put(operationType, registryConfig);
        
        log.debug("已更新操作配置到注册中心: {} -> {}", operationType, registryConfig);
    }
    
    /**
     * 同步模型映射关系
     * 
     * @param operationType 操作类型
     * @param modelName 模型名称
     */
    private void syncModelMapping(String operationType, String modelName) {
        try {
            // 验证模型是否存在
            if (!isValidModel(modelName)) {
                log.warn("模型不存在，跳过映射同步: {} -> {}", operationType, modelName);
                return;
            }
            
            // 设置模型映射
            operationRegistry.setModelForOperation(operationType, modelName);
            log.debug("已同步模型映射: {} -> {}", operationType, modelName);
            
        } catch (Exception e) {
            log.warn("同步模型映射失败: {} -> {} - {}", operationType, modelName, e.getMessage());
            // 模型映射失败不应该阻止整个同步过程
        }
    }
    
    /**
     * 验证模型是否有效
     * 
     * @param modelName 模型名称
     * @return 是否有效
     */
    private boolean isValidModel(String modelName) {
        try {
            // 检查模型是否在模型注册中心中存在
            return modelRegistry.getModel(modelName) != null;
        } catch (Exception e) {
            log.debug("验证模型时出错: {} - {}", modelName, e.getMessage());
            return false;
        }
    }
    
    /**
     * 添加操作配置更新方法，供外部调用
     * 
     * @param operationType 操作类型
     * @param configData 配置数据
     */
    public void updateOperationConfig(String operationType, OperationConfigData configData) {
        try {
            // 验证配置
            if (!configData.isValid()) {
                throw new IllegalArgumentException("无效的操作配置: " + operationType);
            }
            
            // 同步到框架
            syncDatabaseConfigToFramework(operationType, configData);
            
            log.info("操作配置更新成功: {}", operationType);
            
        } catch (Exception e) {
            log.error("更新操作配置失败: {} - {}", operationType, e.getMessage(), e);
            throw new RuntimeException("更新操作配置失败: " + operationType, e);
        }
    }
    
    /**
     * 批量同步所有数据库配置到框架
     */
    public void syncAllDatabaseConfigsToFramework() {
        try {
            Map<String, OperationConfigData> allConfigs = persistenceService.getAllOperationConfigs();
            
            log.info("开始批量同步 {} 个操作配置到框架", allConfigs.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Map.Entry<String, OperationConfigData> entry : allConfigs.entrySet()) {
                try {
                    syncDatabaseConfigToFramework(entry.getKey(), entry.getValue());
                    successCount++;
                } catch (Exception e) {
                    log.error("同步操作配置失败: {} - {}", entry.getKey(), e.getMessage());
                    failCount++;
                }
            }
            
            log.info("批量同步完成 - 成功: {}, 失败: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("批量同步配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量同步配置失败", e);
        }
    }
    
    /**
     * 转换为OpenAI模型配置
     */
    private OpenAIModelConfig convertToOpenAIConfig(ModelConfigData config) {
        return OpenAIModelConfig.builder()
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .defaultMaxTokens(config.getDefaultMaxTokens())
                .defaultTemperature(config.getDefaultTemperature())
                .supportStream(config.getSupportStream())
                .supportJsonOutput(config.getSupportJsonOutput())
                .supportThinking(config.getSupportThinking())
                .additionalHeaders(config.getAdditionalHeaders())
                .description(config.getDescription())
                .provider(config.getProvider())
                .enabled(config.getEnabled())
                .build();
    }
    
    /**
     * 从OpenAI模型配置转换
     */
    private ModelConfigData convertFromOpenAIConfig(OpenAIModelConfig config) {
        ModelConfigData data = new ModelConfigData();
        data.setModelName(config.getModelName());
        data.setBaseUrl(config.getBaseUrl());
        data.setApiKey(config.getApiKey());
        data.setDefaultMaxTokens(config.getDefaultMaxTokens());
        data.setDefaultTemperature(config.getDefaultTemperature());
        data.setSupportStream(config.getSupportStream());
        data.setSupportJsonOutput(config.getSupportJsonOutput());
        data.setSupportThinking(config.getSupportThinking());
        data.setAdditionalHeaders(config.getAdditionalHeaders());
        data.setDescription(config.getDescription());
        data.setProvider(config.getProvider());
        data.setEnabled(config.getEnabled());
        data.updateTimestamp();
        return data;
    }
}
