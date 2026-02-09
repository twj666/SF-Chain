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
 * 描述: 基于PostgreSQL的持久化服务实现
 * 替代原有的JSON文件持久化方案
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sf-chain.persistence.database-type", havingValue = "postgresql", matchIfMissing = true)
public class PostgreSQLPersistenceService implements PersistenceService {
    
    private final ModelConfigRepository modelConfigRepository;
    private final OperationConfigRepository operationConfigRepository;
    
    @Override
    public void saveModelConfig(String modelName, ModelConfigData config) {
        log.debug("保存模型配置: {}", modelName);
        config.setModelName(modelName);
        
        // 查找现有实体并更新，或创建新实体
        ModelConfigEntity entity = modelConfigRepository.findByModelName(modelName)
            .map(existing -> {
                // 更新现有实体，保留 id 和 createdAt
                Long id = existing.getId();
                LocalDateTime createdAt = existing.getCreatedAt();
                
                // 使用新的转换方法，而不是 BeanUtils.copyProperties
                ModelConfigEntity updatedEntity = convertToEntity(config);
                updatedEntity.setId(id);
                updatedEntity.setCreatedAt(createdAt);
                
                return updatedEntity;
            })
            .orElseGet(() -> {
                // 创建新实体，使用新的转换方法
                return convertToEntity(config);
            });
        
        modelConfigRepository.save(entity);
        log.info("模型配置已保存: {}", modelName);
    }
    
    @Override
    public Optional<ModelConfigData> getModelConfig(String modelName) {
        log.debug("加载模型配置: {}", modelName);
        return modelConfigRepository.findByModelName(modelName)
                .map(this::convertToData);
    }
    
    @Override
    public Map<String, ModelConfigData> getAllModelConfigs() {
        log.debug("加载所有模型配置");
        return modelConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ModelConfigEntity::getModelName,
                        this::convertToData
                ));
    }
    
    @Override
    public boolean deleteModelConfig(String modelName) {
        log.debug("删除模型配置: {}", modelName);
        Optional<ModelConfigEntity> entity = modelConfigRepository.findByModelName(modelName);
        if (entity.isPresent()) {
            modelConfigRepository.deleteById(entity.get().getId());
            log.info("模型配置已删除: {}", modelName);
            return true;
        } else {
            log.warn("模型配置不存在，无法删除: {}", modelName);
            return false;
        }
    }
    
    @Override
    public boolean existsModelConfig(String modelName) {
        return modelConfigRepository.existsByModelName(modelName);
    }
    
    @Override
    public List<String> getAllModelNames() {
        log.debug("获取所有模型名称");
        return modelConfigRepository.findEnabledModelNames();
    }
    
    @Override
    public void saveOperationConfig(String operationType, OperationConfigData config) {
        log.debug("保存操作配置: {}", operationType);
        config.setOperationType(operationType);
        
        // 查找现有实体并更新，或创建新实体
        OperationConfigEntity entity = operationConfigRepository.findByOperationType(operationType)
            .map(existing -> {
                // 更新现有实体，保留 id 和 createdAt
                Long id = existing.getId();
                LocalDateTime createdAt = existing.getCreatedAt();
                
                // 转换新的配置数据
                OperationConfigEntity updatedEntity = convertToEntity(config);
                updatedEntity.setId(id);
                updatedEntity.setCreatedAt(createdAt);
                
                return updatedEntity;
            })
            .orElseGet(() -> {
                // 创建新实体
                return convertToEntity(config);
            });
        
        operationConfigRepository.save(entity);
        log.info("操作配置已保存: {}", operationType);
    }
    
    @Override
    public Optional<OperationConfigData> getOperationConfig(String operationType) {
        log.debug("加载操作配置: {}", operationType);
        return operationConfigRepository.findByOperationType(operationType)
                .map(this::convertToData);
    }
    
    @Override
    public Map<String, OperationConfigData> getAllOperationConfigs() {
        log.debug("加载所有操作配置");
        return operationConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
                        OperationConfigEntity::getOperationType,
                        this::convertToData
                ));
    }
    
    @Override
    public boolean deleteOperationConfig(String operationType) {
        log.debug("删除操作配置: {}", operationType);
        Optional<OperationConfigEntity> entity = operationConfigRepository.findByOperationType(operationType);
        if (entity.isPresent()) {
            operationConfigRepository.deleteById(entity.get().getId());
            log.info("操作配置已删除: {}", operationType);
            return true;
        } else {
            log.warn("操作配置不存在，无法删除: {}", operationType);
            return false;
        }
    }
    
    @Override
    public void flush() {
        // PostgreSQL自动提交事务，无需手动flush
        log.debug("PostgreSQL持久化服务flush操作（无需手动操作）");
    }
    
    @Override
    public void reload() {
        // PostgreSQL数据实时同步，无需手动reload
        log.debug("PostgreSQL持久化服务reload操作（数据实时同步）");
    }
    
    @Override
    @Transactional
    public void backup(String backupName) {
        // PostgreSQL备份通常使用pg_dump等工具，这里记录备份请求
        log.info("PostgreSQL备份请求，备份名称: {}。请使用pg_dump等工具进行数据库备份。", backupName);
    }
    
    @Override
    @Transactional
    public void restoreFromBackup(String backupName) {
        // PostgreSQL恢复通常使用pg_restore等工具，这里记录恢复请求
        log.info("PostgreSQL恢复请求，备份名称: {}。请使用pg_restore等工具进行数据库恢复。", backupName);
    }
    
    @Override
    public List<String> getAllBackupNames() {
        // PostgreSQL备份文件管理通常在文件系统层面，这里返回空列表
        log.debug("PostgreSQL备份文件列表查询（需要在文件系统层面管理）");
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