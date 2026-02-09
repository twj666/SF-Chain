package com.suifeng.sfchain.persistence.repository;

import com.suifeng.sfchain.persistence.entity.OperationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 描述: 操作配置Repository接口
 * 用于操作配置的数据库操作
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Repository
public interface OperationConfigRepository extends JpaRepository<OperationConfigEntity, Long> {
    
    /**
     * 根据操作类型查找配置
     * @param operationType 操作类型
     * @return 操作配置
     */
    Optional<OperationConfigEntity> findByOperationType(String operationType);
    
    /**
     * 根据启用状态查找配置
     * @param enabled 启用状态
     * @return 操作配置列表
     */
    List<OperationConfigEntity> findByEnabled(Boolean enabled);
    
    /**
     * 检查操作类型是否存在配置
     * @param operationType 操作类型
     * @return 是否存在
     */
    boolean existsByOperationType(String operationType);
    
    /**
     * 根据操作类型删除配置
     * @param operationType 操作类型
     */
    void deleteByOperationType(String operationType);
    
    /**
     * 根据JSON输出模式查找配置
     * @param jsonOutput JSON输出模式
     * @return 操作配置列表
     */
    List<OperationConfigEntity> findByJsonOutput(Boolean jsonOutput);
    
    /**
     * 根据思考模式查找配置
     * @param thinkingMode 思考模式
     * @return 操作配置列表
     */
    List<OperationConfigEntity> findByThinkingMode(Boolean thinkingMode);
    
    /**
     * 获取所有操作类型
     * @return 操作类型列表
     */
    @Query("SELECT c.operationType FROM OperationConfigEntity c")
    List<String> findAllOperationTypes();
    
    /**
     * 获取所有启用的操作类型
     * @return 启用的操作类型列表
     */
    @Query("SELECT c.operationType FROM OperationConfigEntity c WHERE c.enabled = true")
    List<String> findEnabledOperationTypes();
}