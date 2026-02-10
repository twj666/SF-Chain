package com.suifeng.sfchain.core.logging;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI调用日志摘要 - 用于列表展示，不包含大数据量字段
 */
@Data
@Builder
public class AICallLogSummary {
    
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
    private AICallLog.CallStatus status;
    
    /** 错误信息(如果有) */
    private String errorMessage;
    
    /** 调用频次(用于LFU) */
    private int frequency;
    
    /** 最后访问时间(用于LFU) */
    private LocalDateTime lastAccessTime;
    
    /** AI请求参数摘要 */
    private RequestParamsSummary requestParams;
    
    @Data
    @Builder
    public static class RequestParamsSummary {
        private Integer maxTokens;
        private Double temperature;
        private Boolean jsonOutput;
        private Boolean thinking;
    }
    
    /**
     * 从完整日志创建摘要
     */
    public static AICallLogSummary fromFullLog(AICallLog fullLog) {
        RequestParamsSummary paramsSummary = null;
        if (fullLog.getRequestParams() != null) {
            paramsSummary = RequestParamsSummary.builder()
                    .maxTokens(fullLog.getRequestParams().getMaxTokens())
                    .temperature(fullLog.getRequestParams().getTemperature())
                    .jsonOutput(fullLog.getRequestParams().getJsonOutput())
                    .thinking(fullLog.getRequestParams().getThinking())
                    .build();
        }
        
        return AICallLogSummary.builder()
                .callId(fullLog.getCallId())
                .operationType(fullLog.getOperationType())
                .modelName(fullLog.getModelName())
                .callTime(fullLog.getCallTime())
                .duration(fullLog.getDuration())
                .status(fullLog.getStatus())
                .errorMessage(fullLog.getErrorMessage())
                .frequency(fullLog.getFrequency())
                .lastAccessTime(fullLog.getLastAccessTime())
                .requestParams(paramsSummary)
                .build();
    }
}