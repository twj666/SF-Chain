package com.suifeng.sfchain.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: 操作配置数据类
 * 用于持久化存储的操作配置信息
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationConfigData {
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;
    
    /**
     * 最大token数
     */
    private Integer maxTokens;
    
    /**
     * 温度参数
     */
    private Double temperature;
    
    /**
     * 超时时间（毫秒）
     */
    private Long timeout;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 3;
    
    /**
     * 是否启用JSON输出
     */
    @Builder.Default
    private Boolean jsonOutput = false;
    
    /**
     * 是否启用流式输出
     */
    @Builder.Default
    private Boolean streamOutput = false;
    
    /**
     * 是否启用思考模式
     */
    @Builder.Default
    private Boolean thinkingMode = false;
    
    /**
     * 自定义提示词前缀
     */
    private String promptPrefix;
    
    /**
     * 自定义提示词后缀
     */
    private String promptSuffix;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 输出格式说明
     */
    private String outputFormat;
    
    /**
     * 自定义参数
     */
    @Builder.Default
    private Map<String, Object> customParams = new HashMap<>();
    
    /**
     * 关联的模型名称
     */
    private String modelName;
    
    /**
     * 验证配置是否有效
     * @return 是否有效
     */
    public boolean isValid() {
        // 基本验证：操作类型不能为空
        if (operationType == null || operationType.trim().isEmpty()) {
            return false;
        }
        
        // 验证数值范围
        if (maxTokens != null && maxTokens <= 0) {
            return false;
        }
        
        if (temperature != null && (temperature < 0.0 || temperature > 2.0)) {
            return false;
        }
        
        if (retryCount != null && retryCount < 0) {
            return false;
        }
        
        if (timeout != null && timeout <= 0) {
            return false;
        }
        
        return true;
    }
}