package com.suifeng.sfchain.persistence.repository;

import com.suifeng.sfchain.persistence.entity.ModelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 描述: 模型配置Repository接口
 * 用于模型配置的数据库操作
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfigEntity, Long> {
    
    /**
     * 根据模型名称查找配置
     * @param modelName 模型名称
     * @return 模型配置
     */
    Optional<ModelConfigEntity> findByModelName(String modelName);
    
    /**
     * 根据提供商查找配置
     * @param provider 提供商
     * @return 模型配置列表
     */
    List<ModelConfigEntity> findByProvider(String provider);
    
    /**
     * 根据启用状态查找配置
     * @param enabled 启用状态
     * @return 模型配置列表
     */
    List<ModelConfigEntity> findByEnabled(Boolean enabled);
    
    /**
     * 检查模型名称是否存在配置
     * @param modelName 模型名称
     * @return 是否存在
     */
    boolean existsByModelName(String modelName);
    
    /**
     * 根据模型名称删除配置
     * @param modelName 模型名称
     */
    void deleteByModelName(String modelName);
    
    /**
     * 获取所有提供商
     * @return 提供商列表
     */
    @Query("SELECT DISTINCT m.provider FROM ModelConfigEntity m")
    List<String> findAllProviders();
    
    /**
     * 获取所有启用的模型名称
     * @return 启用的模型名称列表
     */
    @Query("SELECT m.modelName FROM ModelConfigEntity m WHERE m.enabled = true")
    List<String> findEnabledModelNames();
}