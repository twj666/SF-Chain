package com.suifeng.sfchain.core.logging;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI调用日志实体
 */
@Data
@Builder
public class AICallLog {
    
    /** 调用ID */
    private String callId;
    
    /** 操作类型 */
    private String operationType;
    
    /** 模型名称 */
    private String modelName;
    
    /** 调用时间 */
    private LocalDateTime callTime;
    
    /** 执行耗时(毫秒) */
    private long duration;
    
    /** 调用状态 */
    private CallStatus status;
    
    /** 原始输入参数 */
    private Object input;
    
    /** 构建的提示词 */
    private String prompt;
    
    /** AI请求参数 */
    private AIRequestParams requestParams;
    
    /** 模型原始返回结果 */
    private String rawResponse;
    
    /** 最终输出结果 */
    private Object output;
    
    /** 错误信息(如果有) */
    private String errorMessage;
    
    /** 调用频次(用于LFU) */
    private int frequency;
    
    /** 最后访问时间(用于LFU) */
    private LocalDateTime lastAccessTime;
    
    public enum CallStatus {
        SUCCESS, FAILED, TIMEOUT
    }
    
    @Data
    @Builder
    public static class AIRequestParams {
        private Integer maxTokens;
        private Double temperature;
        private Boolean jsonOutput;
        private Boolean thinking;
        private Map<String, Object> additionalParams;
    }
}