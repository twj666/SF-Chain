package com.suifeng.sfchain.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 描述: 持久化服务接口
 * 提供模型配置和操作映射的增删改查功能
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
public interface PersistenceService {
    
    // ==================== 模型配置管理 ====================
    /**
     * 保存模型配置
     * 
     * @param modelName 模型名称
     * @param config 模型配置
     */
    void saveModelConfig(String modelName, ModelConfigData config);
    
    /**
     * 获取模型配置
     * 
     * @param modelName 模型名称
     * @return 模型配置，如果不存在则返回空
     */
    Optional<ModelConfigData> getModelConfig(String modelName);
    
    /**
     * 获取所有模型配置
     * 
     * @return 所有模型配置的映射
     */
    Map<String, ModelConfigData> getAllModelConfigs();
    
    /**
     * 删除模型配置
     * 
     * @param modelName 模型名称
     * @return 是否删除成功
     */
    boolean deleteModelConfig(String modelName);
    
    /**
     * 检查模型配置是否存在
     * 
     * @param modelName 模型名称
     * @return 是否存在
     */
    boolean existsModelConfig(String modelName);
    
    /**
     * 获取所有模型名称
     * 
     * @return 模型名称列表
     */
    List<String> getAllModelNames();
    
    // 移除整个 "操作模型映射管理" 部分
    
    // ==================== 操作配置管理 ====================
    /**
     * 保存操作配置
     * 
     * @param operationType 操作类型
     * @param config 操作配置
     */
    void saveOperationConfig(String operationType, OperationConfigData config);
    
    /**
     * 获取操作配置
     * 
     * @param operationType 操作类型
     * @return 操作配置，如果不存在则返回空
     */
    Optional<OperationConfigData> getOperationConfig(String operationType);
    
    /**
     * 获取所有操作配置
     * 
     * @return 所有操作配置的映射
     */
    Map<String, OperationConfigData> getAllOperationConfigs();
    
    /**
     * 删除操作配置
     * 
     * @param operationType 操作类型
     * @return 是否删除成功
     */
    boolean deleteOperationConfig(String operationType);
    
    // ==================== 数据同步和备份 ====================
    
    /**
     * 刷新数据到持久化存储
     */
    void flush();
    
    /**
     * 从持久化存储重新加载数据
     */
    void reload();
    
    /**
     * 备份当前配置
     * 
     * @param backupName 备份名称
     */
    void backup(String backupName);
    
    /**
     * 从备份恢复配置
     * @param backupName 备份名称
     */
    void restoreFromBackup(String backupName);
    
    /**
     * 获取所有备份名称
     * @return 备份名称列表
     */
    List<String> getAllBackupNames();
}