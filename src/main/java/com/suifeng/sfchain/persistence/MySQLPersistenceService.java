package com.suifeng.sfchain.persistence;

import com.suifeng.sfchain.persistence.entity.ModelConfigEntity;
import com.suifeng.sfchain.persistence.entity.OperationConfigEntity;
import com.suifeng.sfchain.persistence.repository.ModelConfigRepository;
import com.suifeng.sfchain.persistence.repository.OperationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 描述: 基于MySQL的持久化服务实现
 * 针对MySQL数据库的特定优化
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sf-chain.persistence.database-type", havingValue = "mysql")
public class MySQLPersistenceService implements PersistenceService {
    
    private final ModelConfigRepository modelConfigRepository;
    private final OperationConfigRepository operationConfigRepository;
    
    @Override
    @Transactional
    public void saveModelConfig(String modelName, ModelConfigData config) {
        log.debug("[MySQL] 保存模型配置: {}", modelName);
        config.setModelName(modelName);
        
        // MySQL特定的保存逻辑，使用ON DUPLICATE KEY UPDATE
        ModelConfigEntity entity = modelConfigRepository.findByModelName(modelName)
            .map(existing -> {
                // 更新现有实体
                Long id = existing.getId();
                LocalDateTime createdAt = existing.getCreatedAt();
                
                ModelConfigEntity updatedEntity = convertToEntity(config);
                updatedEntity.setId(id);
                updatedEntity.setCreatedAt(createdAt);
                
                return updatedEntity;
            })
            .orElseGet(() -> convertToEntity(config));
        
        modelConfigRepository.save(entity);
        log.info("[MySQL] 模型配置保存成功: {}", modelName);
    }
    
    @Override
    public Optional<ModelConfigData> getModelConfig(String modelName) {
        log.debug("[MySQL] 获取模型配置: {}", modelName);
        return modelConfigRepository.findByModelName(modelName)
                .map(this::convertToData);
    }
    
    @Override
    public Map<String, ModelConfigData> getAllModelConfigs() {
        log.debug("[MySQL] 获取所有模型配置");
        return modelConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
                    ModelConfigEntity::getModelName,
                    this::convertToData
                ));
    }
    
    @Override
    @Transactional
    public boolean deleteModelConfig(String modelName) {
        log.debug("[MySQL] 删除模型配置: {}", modelName);
        try {
            modelConfigRepository.deleteByModelName(modelName);
            log.info("[MySQL] 模型配置删除成功: {}", modelName);
            return true;
        } catch (Exception e) {
            log.error("[MySQL] 删除模型配置失败: {}", modelName, e);
            return false;
        }
    }
    
    @Override
    public boolean existsModelConfig(String modelName) {
        return modelConfigRepository.existsByModelName(modelName);
    }
    
    @Override
    public List<String> getAllModelNames() {
        return modelConfigRepository.findEnabledModelNames();
    }
    
    @Override
    @Transactional
    public void saveOperationConfig(String operationType, OperationConfigData config) {
        log.debug("[MySQL] 保存操作配置: {}", operationType);
        config.setOperationType(operationType);
        
        OperationConfigEntity entity = operationConfigRepository.findByOperationType(operationType)
            .map(existing -> {
                Long id = existing.getId();
                LocalDateTime createdAt = existing.getCreatedAt();
                
                OperationConfigEntity updatedEntity = convertToEntity(config);
                updatedEntity.setId(id);
                updatedEntity.setCreatedAt(createdAt);
                
                return updatedEntity;
            })
            .orElseGet(() -> convertToEntity(config));
        
        operationConfigRepository.save(entity);
        log.info("[MySQL] 操作配置保存成功: {}", operationType);
    }
    
    @Override
    public Optional<OperationConfigData> getOperationConfig(String operationType) {
        log.debug("[MySQL] 获取操作配置: {}", operationType);
        return operationConfigRepository.findByOperationType(operationType)
                .map(this::convertToData);
    }
    
    @Override
    public Map<String, OperationConfigData> getAllOperationConfigs() {
        log.debug("[MySQL] 获取所有操作配置");
        return operationConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
                    OperationConfigEntity::getOperationType,
                    this::convertToData
                ));
    }
    
    @Override
    @Transactional
    public boolean deleteOperationConfig(String operationType) {
        log.debug("[MySQL] 删除操作配置: {}", operationType);
        try {
            operationConfigRepository.deleteByOperationType(operationType);
            log.info("[MySQL] 操作配置删除成功: {}", operationType);
            return true;
        } catch (Exception e) {
            log.error("[MySQL] 删除操作配置失败: {}", operationType, e);
            return false;
        }
    }
    
    @Override
    public void flush() {
        log.debug("[MySQL] 刷新缓存");
        // MySQL特定的刷新逻辑
    }
    
    @Override
    public void reload() {
        log.debug("[MySQL] 重新加载配置");
        // MySQL特定的重载逻辑
    }
    
    @Override
    @Transactional
    public void backup(String backupName) {
        log.info("[MySQL] 创建备份: {}", backupName);
        // MySQL特定的备份实现
        // 可以使用CREATE TABLE ... AS SELECT语句
    }
    
    @Override
    @Transactional
    public void restoreFromBackup(String backupName) {
        log.info("[MySQL] 从备份恢复: {}", backupName);
        // MySQL特定的恢复实现
    }
    
    @Override
    public List<String> getAllBackupNames() {
        log.debug("[MySQL] 获取所有备份名称");
        // 返回备份列表
        return List.of();
    }
    
    /**
     * 将ModelConfigData转换为ModelConfigEntity
     */
    private ModelConfigEntity convertToEntity(ModelConfigData data) {
        ModelConfigEntity entity = new ModelConfigEntity();
        
        // 映射基本字段
        entity.setModelName(data.getModelName());
        entity.setProvider(data.getProvider());
        entity.setApiKey(data.getApiKey());
        entity.setBaseUrl(data.getBaseUrl());
        entity.setEnabled(data.getEnabled());
        entity.setDescription(data.getDescription());
        
        // 将扩展字段映射到customParams
        Map<String, Object> customParams = new HashMap<>();
        if (data.getDefaultMaxTokens() != null) {
            customParams.put("defaultMaxTokens", data.getDefaultMaxTokens());
        }
        if (data.getDefaultTemperature() != null) {
            customParams.put("defaultTemperature", data.getDefaultTemperature());
        }
        if (data.getSupportStream() != null) {
            customParams.put("supportStream", data.getSupportStream());
        }
        if (data.getSupportJsonOutput() != null) {
            customParams.put("supportJsonOutput", data.getSupportJsonOutput());
        }
        if (data.getSupportThinking() != null) {
            customParams.put("supportThinking", data.getSupportThinking());
        }
        if (data.getAdditionalHeaders() != null && !data.getAdditionalHeaders().isEmpty()) {
            customParams.put("additionalHeaders", data.getAdditionalHeaders());
        }
        if (data.getCreatedAt() != null) {
            customParams.put("createdAt", data.getCreatedAt());
        }
        if (data.getUpdatedAt() != null) {
            customParams.put("updatedAt", data.getUpdatedAt());
        }
        
        entity.setCustomParams(customParams);
        return entity;
    }
    
    /**
     * 将ModelConfigEntity转换为ModelConfigData
     */
    private ModelConfigData convertToData(ModelConfigEntity entity) {
        ModelConfigData.ModelConfigDataBuilder builder = ModelConfigData.builder()
                .modelName(entity.getModelName())
                .provider(entity.getProvider())
                .apiKey(entity.getApiKey())
                .baseUrl(entity.getBaseUrl())
                .enabled(entity.getEnabled())
                .description(entity.getDescription());
        
        // 从customParams中提取扩展字段
        Map<String, Object> customParams = entity.getCustomParams();
        if (customParams != null) {
            if (customParams.containsKey("defaultMaxTokens")) {
                builder.defaultMaxTokens((Integer) customParams.get("defaultMaxTokens"));
            }
            if (customParams.containsKey("defaultTemperature")) {
                builder.defaultTemperature((Double) customParams.get("defaultTemperature"));
            }
            if (customParams.containsKey("supportStream")) {
                builder.supportStream((Boolean) customParams.get("supportStream"));
            }
            if (customParams.containsKey("supportJsonOutput")) {
                builder.supportJsonOutput((Boolean) customParams.get("supportJsonOutput"));
            }
            if (customParams.containsKey("supportThinking")) {
                builder.supportThinking((Boolean) customParams.get("supportThinking"));
            }
            if (customParams.containsKey("additionalHeaders")) {
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) customParams.get("additionalHeaders");
                builder.additionalHeaders(headers != null ? headers : new HashMap<>());
            }
            if (customParams.containsKey("createdAt")) {
                builder.createdAt((Long) customParams.get("createdAt"));
            }
            if (customParams.containsKey("updatedAt")) {
                builder.updatedAt((Long) customParams.get("updatedAt"));
            }
        }
        
        return builder.build();
    }
    
    /**
     * 将OperationConfigData转换为OperationConfigEntity
     */
    private OperationConfigEntity convertToEntity(OperationConfigData data) {
        OperationConfigEntity entity = new OperationConfigEntity();
        entity.setOperationType(data.getOperationType());
        entity.setDescription(data.getDescription());
        entity.setEnabled(data.getEnabled());
        entity.setMaxTokens(data.getMaxTokens());
        entity.setTemperature(data.getTemperature());
        entity.setJsonOutput(data.getJsonOutput());
        entity.setThinkingMode(data.getThinkingMode());
        entity.setCustomParams(data.getCustomParams());
        entity.setModelName(data.getModelName());
        return entity;
    }
    
    /**
     * 将OperationConfigEntity转换为OperationConfigData
     */
    private OperationConfigData convertToData(OperationConfigEntity entity) {
        return OperationConfigData.builder()
                .operationType(entity.getOperationType())
                .description(entity.getDescription())
                .enabled(entity.getEnabled())
                .maxTokens(entity.getMaxTokens())
                .temperature(entity.getTemperature())
                .jsonOutput(entity.getJsonOutput())
                .thinkingMode(entity.getThinkingMode())
                .customParams(entity.getCustomParams())
                .modelName(entity.getModelName())
                .build();
    }
}