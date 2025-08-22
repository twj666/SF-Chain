package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import com.suifeng.sfchain.core.openai.OpenAIModelFactory;
import com.suifeng.sfchain.operations.ModelValidationOperation;
import com.suifeng.sfchain.persistence.ModelConfigData;
import com.suifeng.sfchain.persistence.PersistenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.suifeng.sfchain.constants.AIOperationConstant.MODEL_VALIDATION_OP;

/**
 * 描述: AI模型配置管理控制器
 * 提供AI模型的增删改查、测试验证等功能
 * 
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@RestController
@RequestMapping("/sf-chain/models")
@RequiredArgsConstructor
public class AIModelController {
    
    private final PersistenceManager persistenceManager;
    private final AIOperationRegistry operationRegistry;
    private final OpenAIModelFactory modelFactory;
    
    /**
     * 获取所有模型配置（包含状态信息）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllModels() {
        try {
            Map<String, ModelConfigData> models = persistenceManager.getAllModelConfigs();
            Map<String, Object> result = new HashMap<>();
            
            // 按提供商分组
            Map<String, List<ModelConfigData>> groupedByProvider = models.values().stream()
                .collect(Collectors.groupingBy(m -> m.getProvider() != null ? m.getProvider() : "未知"));
            
            result.put("models", models);
            result.put("groupedByProvider", groupedByProvider);
            result.put("total", models.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取模型列表失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取模型列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个模型配置
     */
    @GetMapping("/{modelName}")
    public ResponseEntity<Object> getModel(@PathVariable String modelName) {
        try {
            Map<String, ModelConfigData> models = persistenceManager.getAllModelConfigs();
            ModelConfigData model = models.get(modelName);
            
            if (model == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(model);
        } catch (Exception e) {
            log.error("获取模型配置失败: {} - {}", modelName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取模型配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建或更新模型配置
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveModel(
            @Valid @RequestBody ModelConfigData config) {
        Map<String, Object> result = new HashMap<>();
        String modelName = config.getModelName();
        try {
            // 检查模型是否已存在
            Map<String, ModelConfigData> existingModels = persistenceManager.getAllModelConfigs();
            boolean modelExists = existingModels.containsKey(modelName);
            
            // 验证模型配置（临时验证，不影响现有模型）
            boolean validationResult = validateModelConfig(modelName, config, true);
            
            if (!validationResult) {
                result.put("success", false);
                result.put("message", "模型验证失败，请检查配置参数");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 根据模型是否存在选择添加或更新
            if (modelExists) {
                persistenceManager.updateModelConfig(modelName, config);
                result.put("operation", "updated");
                result.put("message", "模型配置更新成功");
                log.info("模型配置已更新: {}", modelName);
            } else {
                persistenceManager.addModelConfig(modelName, config);
                result.put("operation", "created");
                result.put("message", "模型配置创建成功");
                log.info("模型配置已创建: {}", modelName);
            }
            
            result.put("success", true);
            result.put("modelName", modelName);
            result.put("validated", true);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("保存模型配置失败: {} - {}", modelName, e.getMessage());
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 测试模型连接
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testModel(@RequestBody Map<String, String> request) {
        String modelName = request.get("modelName");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, ModelConfigData> models = persistenceManager.getAllModelConfigs();
            ModelConfigData config = models.get(modelName);
            
            if (config == null) {
                result.put("success", false);
                result.put("message", "模型配置不存在");
                return ResponseEntity.notFound().build();
            }
            
            // 测试已存在的模型，不需要临时注册
            boolean testResult = validateModelConfig(modelName, config, false);
            result.put("success", testResult);
            result.put("message", testResult ? "模型连接测试成功" : "模型连接测试失败");
            result.put("modelName", modelName);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试模型连接失败: {} - {}", modelName, e.getMessage());
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 验证模型配置是否可用
     * @param modelName 模型名称
     * @param config 模型配置
     * @param isTemporaryValidation 是否为临时验证（true: 验证后移除临时模型，false: 验证已存在的模型）
     */
    private boolean validateModelConfig(String modelName, ModelConfigData config, boolean isTemporaryValidation) {
        boolean tempRegistered = false;
        try {
            log.info("开始验证模型配置: {} (临时验证: {})", modelName, isTemporaryValidation);
            
            ModelValidationOperation validationOp = (ModelValidationOperation) operationRegistry.getOperation(MODEL_VALIDATION_OP);
            if (validationOp == null) {
                log.warn("未找到模型验证操作，跳过验证");
                return true;
            }
            
            // 如果是临时验证，需要临时注册模型
            if (isTemporaryValidation) {
                OpenAIModelConfig tempConfig = convertToOpenAIConfig(config);
                modelFactory.registerModel(tempConfig);
                tempRegistered = true;
            }
            
            ModelValidationOperation.ValidationRequest request = 
                new ModelValidationOperation.ValidationRequest("请回答：2+3等于几？");
            
            ModelValidationOperation.ValidationResult result = validationOp.execute(request, modelName);
            
            return result != null && result.getAnswer() != null && !result.getAnswer().trim().isEmpty();
            
        } catch (Exception e) {
            log.error("模型验证失败: {} - {}", modelName, e.getMessage());
            return false;
        } finally {
            // 只有在临时验证时才清理临时模型
            if (tempRegistered && isTemporaryValidation) {
                try {
                    modelFactory.removeModel(modelName);
                    log.debug("已清理临时模型配置: {}", modelName);
                } catch (Exception e) {
                    log.warn("清理临时模型配置失败: {}", e.getMessage());
                }
            }
        }
    }

    
    /**
     * 删除模型配置
     */
    @DeleteMapping("/{modelName}")
    public ResponseEntity<Map<String, Object>> deleteModel(@PathVariable String modelName) {
        Map<String, Object> result = new HashMap<>();
        try {
            persistenceManager.deleteModelConfig(modelName);
            result.put("success", true);
            result.put("message", "模型配置删除成功");
            result.put("modelName", modelName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("删除模型配置失败: {} - {}", modelName, e.getMessage());
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 转换配置格式
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
}