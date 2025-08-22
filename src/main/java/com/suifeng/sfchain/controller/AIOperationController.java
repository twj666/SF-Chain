package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.persistence.ModelConfigData;
import com.suifeng.sfchain.persistence.OperationConfigData;
import com.suifeng.sfchain.persistence.PersistenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 描述: AI操作配置管理控制器
 * 提供AI操作的配置管理、模型映射等功能
 * 
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@RestController
@RequestMapping("/sf-chain/operations")
@RequiredArgsConstructor
public class AIOperationController {
    
    private final PersistenceManager persistenceManager;
    
    /**
     * 获取所有AI操作及其配置状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOperations() {
        try {
            Map<String, OperationConfigData> configs = persistenceManager.getAllOperationConfigs();
            
            // 构建操作模型映射信息（从操作配置中提取）
            Map<String, String> mappings = new HashMap<>();
            configs.forEach((operationType, config) -> {
                if (config.getModelName() != null && !config.getModelName().isEmpty()) {
                    mappings.put(operationType, config.getModelName());
                }
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("mappings", mappings);
            result.put("configs", configs);
            result.put("totalOperations", configs.size());
            result.put("configuredOperations", mappings.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取操作列表失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取操作列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取操作配置（包含关联的模型信息）
     */
    @GetMapping("/{operationType}")
    public ResponseEntity<Object> getOperation(@PathVariable String operationType) {
        try {
            Optional<OperationConfigData> operationOpt = persistenceManager.getOperationConfig(operationType);
            
            if (operationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            OperationConfigData operation = operationOpt.get();
            
            // 如果有关联模型，获取模型信息
            if (operation.getModelName() != null) {
                Map<String, ModelConfigData> models = persistenceManager.getAllModelConfigs();
                ModelConfigData model = models.get(operation.getModelName());
                
                Map<String, Object> result = new HashMap<>();
                result.put("operation", operation);
                result.put("associatedModel", model);
                return ResponseEntity.ok(result);
            }
            
            return ResponseEntity.ok(operation);
        } catch (Exception e) {
            log.error("获取操作配置失败: {} - {}", operationType, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取操作配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 保存操作配置 - 改为统一的save接口，从请求体获取operationType
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveOperationConfig(
            @Valid @RequestBody OperationConfigData config) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从config对象中获取operationType
            String operationType = config.getOperationType();
            
            if (operationType == null || operationType.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "操作类型不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            persistenceManager.saveOperationConfig(operationType, config);
            
            result.put("success", true);
            result.put("message", "操作配置保存成功");
            result.put("operationType", operationType);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("保存操作配置失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取单个操作配置 - 改为POST请求体参数
     */
    @PostMapping("/get")
    public ResponseEntity<Object> getOperation(@RequestBody Map<String, String> request) {
        try {
            String operationType = request.get("operationType");
            
            if (operationType == null || operationType.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "操作类型不能为空"));
            }
            
            Optional<OperationConfigData> operationOpt = persistenceManager.getOperationConfig(operationType);
            
            if (operationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            OperationConfigData operation = operationOpt.get();
            
            // 如果有关联模型，获取模型信息
            if (operation.getModelName() != null) {
                Map<String, ModelConfigData> models = persistenceManager.getAllModelConfigs();
                ModelConfigData model = models.get(operation.getModelName());
                
                Map<String, Object> result = new HashMap<>();
                result.put("operation", operation);
                result.put("associatedModel", model);
                return ResponseEntity.ok(result);
            }
            
            return ResponseEntity.ok(operation);
        } catch (Exception e) {
            log.error("获取操作配置失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取操作配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量设置操作模型映射
     */
    @PostMapping("/mappings")
    public ResponseEntity<Map<String, Object>> setOperationMappings(
            @RequestBody Map<String, String> mappings) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        Map<String, String> errors = new HashMap<>();
        
        try {
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                try {
                    String operationType = entry.getKey();
                    String modelName = entry.getValue();
                    
                    // 获取现有操作配置
                    Optional<OperationConfigData> configOpt = persistenceManager.getOperationConfig(operationType);
                    OperationConfigData config;
                    
                    if (configOpt.isPresent()) {
                        config = configOpt.get();
                        config.setModelName(modelName);
                    } else {
                        // 创建新的操作配置
                        config = new OperationConfigData();
                        config.setModelName(modelName);
                        config.setEnabled(true);
                        config.setDescription("通过映射设置创建的配置");
                    }
                    
                    persistenceManager.saveOperationConfig(operationType, config);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.put(entry.getKey(), e.getMessage());
                }
            }
            
            result.put("success", failCount == 0);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("total", mappings.size());
            
            if (failCount > 0) {
                result.put("errors", errors);
                result.put("message", "部分操作映射设置失败");
            } else {
                result.put("message", "所有操作映射设置成功");
            }
            
            return failCount > 0 ? ResponseEntity.badRequest().body(result) : ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("设置操作映射失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "设置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 设置单个操作模型映射 - 改为POST请求体参数
     */
    @PostMapping("/mapping")
    public ResponseEntity<Map<String, Object>> setOperationMapping(
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String operationType = request.get("operationType");
            String modelName = request.get("modelName");
            
            if (operationType == null || operationType.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "操作类型不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            if (modelName == null || modelName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "模型名称不能为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 获取现有操作配置
            Optional<OperationConfigData> configOpt = persistenceManager.getOperationConfig(operationType);
            OperationConfigData config;
            
            if (configOpt.isPresent()) {
                config = configOpt.get();
                config.setModelName(modelName);
            } else {
                // 创建新的操作配置
                config = new OperationConfigData();
                config.setModelName(modelName);
                config.setEnabled(true);
                config.setDescription("通过映射设置创建的配置");
            }
            
            persistenceManager.saveOperationConfig(operationType, config);
            
            result.put("success", true);
            result.put("message", "操作映射设置成功");
            result.put("operationType", operationType);
            result.put("modelName", modelName);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("设置操作映射失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "设置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}