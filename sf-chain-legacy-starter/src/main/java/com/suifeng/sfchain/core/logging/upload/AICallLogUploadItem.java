package com.suifeng.sfchain.core.logging.upload;

import com.suifeng.sfchain.core.logging.AICallLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上报到配置中心的AI调用日志
 */
@Data
@Builder
public class AICallLogUploadItem {
    private String callId;
    private String operationType;
    private String modelName;
    private LocalDateTime callTime;
    private long duration;
    private String status;
    private String errorMessage;
    private Integer maxTokens;
    private Double temperature;
    private Boolean jsonOutput;
    private Boolean thinking;
    private Object input;
    private String prompt;
    private String rawResponse;
    private Object output;

    public static AICallLogUploadItem from(AICallLog callLog, boolean uploadContent) {
        AICallLog.AIRequestParams params = callLog.getRequestParams();
        return AICallLogUploadItem.builder()
                .callId(callLog.getCallId())
                .operationType(callLog.getOperationType())
                .modelName(callLog.getModelName())
                .callTime(callLog.getCallTime())
                .duration(callLog.getDuration())
                .status(callLog.getStatus() == null ? null : callLog.getStatus().name())
                .errorMessage(callLog.getErrorMessage())
                .maxTokens(params == null ? null : params.getMaxTokens())
                .temperature(params == null ? null : params.getTemperature())
                .jsonOutput(params == null ? null : params.getJsonOutput())
                .thinking(params == null ? null : params.getThinking())
                .input(uploadContent ? callLog.getInput() : null)
                .prompt(uploadContent ? callLog.getPrompt() : null)
                .rawResponse(uploadContent ? callLog.getRawResponse() : null)
                .output(uploadContent ? callLog.getOutput() : null)
                .build();
    }
}
