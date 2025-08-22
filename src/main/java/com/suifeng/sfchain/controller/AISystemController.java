package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.persistence.ModelConfigData;
import com.suifeng.sfchain.persistence.OperationConfigData;
import com.suifeng.sfchain.persistence.PersistenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: AI系统管理控制器
 * 提供系统概览、备份、刷新、重置等系统级功能
 * 
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@RestController
@RequestMapping("/sf-chain/system")
@RequiredArgsConstructor
public class AISystemController {
    
    private final PersistenceManager persistenceManager;
    
    /**
     * 获取AI系统概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        try {
            // 模型统计
            Map<String, ModelConfigData> models = persistenceManager.getAllModelConfigs();
            overview.put("totalModels", models.size());
            overview.put("enabledModels", models.values().stream()
                .mapToInt(m -> Boolean.TRUE.equals(m.getEnabled()) ? 1 : 0).sum());
            
            // 操作统计
            Map<String, OperationConfigData> configs = persistenceManager.getAllOperationConfigs();
            overview.put("totalOperations", configs.size());
            overview.put("configuredOperations", configs.values().stream()
                .mapToInt(config -> config.getModelName() != null && !config.getModelName().isEmpty() ? 1 : 0).sum());
            
            // 配置统计
            overview.put("totalConfigs", configs.size());
            
            // 系统状态
            overview.put("systemStatus", "running");
            overview.put("lastUpdate", System.currentTimeMillis());
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("获取系统概览失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取系统概览失败: " + e.getMessage()));
        }
    }
    
    /**
     * 系统配置备份
     */
    @PostMapping("/backup")
    public ResponseEntity<Map<String, Object>> createBackup() {
        Map<String, Object> result = new HashMap<>();
        try {
            String backupName = "backup_" + System.currentTimeMillis();
            persistenceManager.createBackup(backupName);
            
            result.put("success", true);
            result.put("message", "配置备份创建成功");
            result.put("backupName", backupName);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("创建备份失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "备份失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 刷新系统配置
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshSystem() {
        Map<String, Object> result = new HashMap<>();
        try {
            persistenceManager.flushConfigurations();
            
            result.put("success", true);
            result.put("message", "系统配置刷新成功");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("刷新系统配置失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "刷新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 重置系统配置
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSystem() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 重新加载配置（替代原来的resetOperationMappingsToDefault方法）
            persistenceManager.reloadConfigurations();
            
            result.put("success", true);
            result.put("message", "系统配置重置成功");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("重置系统配置失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "重置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}