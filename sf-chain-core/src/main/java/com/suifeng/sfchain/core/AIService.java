package com.suifeng.sfchain.core;

import com.suifeng.sfchain.persistence.context.ChatContextService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: AI服务类 - 新框架版本
 * 统一管理AI操作的执行
 *
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
@Service
public class AIService {

    @Resource
    private AIOperationRegistry operationRegistry;

    @Resource
    private ChatContextService chatContextService;

    /**
     * 操作执行统计
     */
    private final Map<String, ExecutionStats> executionStats = new ConcurrentHashMap<>();

    /**
     * 执行AI操作
     *
     * @param operationType 操作类型
     * @param input 输入参数
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> OUTPUT execute(String operationType, INPUT input) {
        return execute(operationType, input, null, null);
    }


    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> OUTPUT execute(String operationType, INPUT input, String sessionId) {
        return execute(operationType, input,null, sessionId);
    }

    /**
     * 执行AI操作（带上下文支持）
     *
     * @param operationType 操作类型
     * @param input 输入参数
     * @param modelName 指定的模型名称
     * @param sessionId 会话ID，用于上下文管理
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> OUTPUT execute(String operationType, INPUT input, String modelName, String sessionId) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取操作实例
            BaseAIOperation<INPUT, OUTPUT> operation = (BaseAIOperation<INPUT, OUTPUT>) operationRegistry.getOperation(operationType);

            // 检查操作是否启用
            if (!operation.isEnabled()) {
                throw new IllegalStateException("操作已禁用: " + operationType);
            }

            // 如果有会话ID，记录用户输入到上下文
            if (sessionId != null && input != null) {
                chatContextService.addUserMessage(sessionId, input.toString());
            }

            // 执行操作
            OUTPUT result = operation.execute(input, modelName, sessionId);

            // 如果有会话ID，记录AI回复到上下文
            if (sessionId != null && result != null) {
                chatContextService.addAiResponse(sessionId, result.toString());
            }

            // 记录执行统计
            recordExecution(operationType, true, System.currentTimeMillis() - startTime);

            log.debug("AI操作执行成功: {} - 耗时: {}ms", operationType, System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            // 记录执行统计
            recordExecution(operationType, false, System.currentTimeMillis() - startTime);

            log.error("AI操作执行失败: {} - {}", operationType, e.getMessage(), e);
            throw new RuntimeException("AI操作执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置会话系统提示词
     *
     * @param sessionId 会话ID
     * @param systemPrompt 系统提示词
     */
    public void setSystemPrompt(String sessionId, String systemPrompt) {
        chatContextService.setSystemPrompt(sessionId, systemPrompt);
        log.info("设置会话系统提示词: sessionId={}", sessionId);
    }

    /**
     * 获取会话上下文
     *
     * @param sessionId 会话ID
     * @param includeSystemPrompt 是否包含系统提示词
     * @return 上下文字符串
     */
    public String getSessionContext(String sessionId, boolean includeSystemPrompt) {
        return chatContextService.getContextAsString(sessionId, includeSystemPrompt);
    }

    /**
     * 清除会话对话历史
     *
     * @param sessionId 会话ID
     */
    public void clearSessionConversation(String sessionId) {
        chatContextService.clearConversation(sessionId);
        log.info("清除会话对话历史: sessionId={}", sessionId);
    }

    /**
     * 完全清除会话
     *
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        chatContextService.clearSession(sessionId);
        log.info("完全清除会话: sessionId={}", sessionId);
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        return chatContextService.sessionExists(sessionId);
    }

    /**
     * 异步执行AI操作
     *
     * @param operationType 操作类型
     * @param input 输入参数
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 异步执行结果
     */
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> CompletableFuture<OUTPUT> executeAsync(String operationType, INPUT input) {
        return executeAsync(operationType, input, null, null);
    }

    /**
     * 异步执行AI操作（指定模型）
     *
     * @param operationType 操作类型
     * @param input 输入参数
     * @param modelName 指定的模型名称
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 异步执行结果
     */
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> CompletableFuture<OUTPUT> executeAsync(String operationType, INPUT input, String modelName) {
        return executeAsync(operationType, input, modelName, null);
    }

    /**
     * 异步执行AI操作（带上下文支持）
     *
     * @param operationType 操作类型
     * @param input 输入参数
     * @param modelName 指定的模型名称
     * @param sessionId 会话ID
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 异步执行结果
     */
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> CompletableFuture<OUTPUT> executeAsync(String operationType, INPUT input, String modelName, String sessionId) {
        return CompletableFuture.supplyAsync(() -> execute(operationType, input, modelName, sessionId));
    }

    /**
     * 流式执行AI操作
     */
    @SuppressWarnings("unchecked")
    public <INPUT> Flux<String> executeStream(String operationType, INPUT input) {
        return executeStream(operationType, input, null, null);
    }

    /**
     * 流式执行AI操作（指定模型）
     */
    @SuppressWarnings("unchecked")
    public <INPUT> Flux<String> executeStream(String operationType, INPUT input, String modelName) {
        return executeStream(operationType, input, modelName, null);
    }

    /**
     * 流式执行AI操作（带上下文支持）
     */
    @SuppressWarnings("unchecked")
    public <INPUT> Flux<String> executeStream(String operationType, INPUT input, String modelName, String sessionId) {
        try {
            // 获取操作实例
            BaseAIOperation<INPUT, ?> operation = (BaseAIOperation<INPUT, ?>) operationRegistry.getOperation(operationType);
            
            // 检查操作是否启用
            if (!operation.isEnabled()) {
                return Flux.error(new IllegalStateException("操作已禁用: " + operationType));
            }
            
            // 如果有会话ID，记录用户输入到上下文
            if (sessionId != null && input != null) {
                chatContextService.addUserMessage(sessionId, input.toString());
            }
            
            // 用于收集流式响应的StringBuilder
            StringBuilder responseBuilder = new StringBuilder();
            
            // 执行流式操作 - 修复参数传递
            return operation.executeStream(input, modelName, sessionId)
                .doOnNext(chunk -> {
                    // 收集每个响应片段
                    if (sessionId != null && chunk != null) {
                        responseBuilder.append(chunk);
                    }
                })
                .doOnSubscribe(subscription -> {
                    log.debug("开始流式执行AI操作: {}", operationType);
                })
                .doOnComplete(() -> {
                    // 流完成时，将完整的AI响应添加到上下文
                    if (sessionId != null && responseBuilder.length() > 0) {
                        chatContextService.addAiResponse(sessionId, responseBuilder.toString());
                    }
                    log.debug("流式AI操作执行完成: {}", operationType);
                })
                .doOnError(error -> {
                    log.error("流式AI操作执行失败: {} - {}", operationType, error.getMessage(), error);
                });
                
        } catch (Exception e) {
            log.error("流式AI操作执行失败: {} - {}", operationType, e.getMessage(), e);
            return Flux.error(new RuntimeException("流式AI操作执行失败: " + e.getMessage(), e));
        }
    }

    /**
     * 批量执行AI操作
     *
     * @param operationType 操作类型
     * @param inputs 输入参数列表
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 执行结果列表
     */
    public <INPUT, OUTPUT> List<OUTPUT> executeBatch(String operationType, List<INPUT> inputs) {
        return executeBatch(operationType, inputs, null);
    }

    /**
     * 批量执行AI操作（指定模型）
     *
     * @param operationType 操作类型
     * @param inputs 输入参数列表
     * @param modelName 指定的模型名称
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 执行结果列表
     */
    public <INPUT, OUTPUT> List<OUTPUT> executeBatch(String operationType, List<INPUT> inputs, String modelName) {
        return inputs.parallelStream()
                .map(input -> this.<INPUT, OUTPUT>execute(operationType, input, modelName))
                .toList();
    }

    /**
     * 异步批量执行AI操作
     *
     * @param operationType 操作类型
     * @param inputs 输入参数列表
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 异步执行结果列表
     */
    public <INPUT, OUTPUT> CompletableFuture<List<OUTPUT>> executeBatchAsync(String operationType, List<INPUT> inputs) {
        return executeBatchAsync(operationType, inputs, null);
    }

    /**
     * 异步批量执行AI操作（指定模型）
     *
     * @param operationType 操作类型
     * @param inputs 输入参数列表
     * @param modelName 指定的模型名称
     * @param <INPUT> 输入类型
     * @param <OUTPUT> 输出类型
     * @return 异步执行结果列表
     */
    public <INPUT, OUTPUT> CompletableFuture<List<OUTPUT>> executeBatchAsync(String operationType, List<INPUT> inputs, String modelName) {
        List<CompletableFuture<OUTPUT>> futures = inputs.stream()
                .map(input -> this.<INPUT, OUTPUT>executeAsync(operationType, input, modelName))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * 获取所有可用的操作
     *
     * @return 操作类型列表
     */
    public List<String> getAvailableOperations() {
        return operationRegistry.getAllOperations();
    }

    /**
     * 检查操作是否可用
     *
     * @param operationType 操作类型
     * @return 是否可用
     */
    public boolean isOperationAvailable(String operationType) {
        try {
            BaseAIOperation<?, ?> operation = operationRegistry.getOperation(operationType);
            return operation != null && operation.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取操作信息
     *
     * @param operationType 操作类型
     * @return 操作信息
     */
    public OperationInfo getOperationInfo(String operationType) {
        BaseAIOperation<?, ?> operation = operationRegistry.getOperation(operationType);
        if (operation == null) {
            return null;
        }

        return OperationInfo.builder()
                .operationType(operationType)
                .description(operation.getDescription())
                .inputType(operation.getInputType())
                .outputType(operation.getOutputType())
                .enabled(operation.isEnabled())
                .supportedModels(operation.getSupportedModels())
                .defaultModel(operation.getAnnotation().defaultModel())
                .build();
    }

    /**
     * 获取操作执行统计
     *
     * @param operationType 操作类型
     * @return 执行统计
     */
    public ExecutionStats getExecutionStats(String operationType) {
        return executionStats.getOrDefault(operationType, new ExecutionStats());
    }

    /**
     * 获取所有操作的执行统计
     *
     * @return 执行统计映射
     */
    public Map<String, ExecutionStats> getAllExecutionStats() {
        return Map.copyOf(executionStats);
    }

    /**
     * 清空执行统计
     */
    public void clearExecutionStats() {
        executionStats.clear();
    }

    /**
     * 记录执行统计
     *
     * @param operationType 操作类型
     * @param success 是否成功
     * @param duration 执行时长
     */
    private void recordExecution(String operationType, boolean success, long duration) {
        executionStats.computeIfAbsent(operationType, k -> new ExecutionStats())
                .record(success, duration);
    }

    /**
     * 操作信息类
     */
    @Getter
    public static class OperationInfo {
        // Getters
        private String operationType;
        private String description;
        private Class<?> inputType;
        private Class<?> outputType;
        private boolean enabled;
        private String[] supportedModels;
        private String defaultModel;

        public static OperationInfoBuilder builder() {
            return new OperationInfoBuilder();
        }

        public static class OperationInfoBuilder {
            private OperationInfo info = new OperationInfo();

            public OperationInfoBuilder operationType(String operationType) {
                info.operationType = operationType;
                return this;
            }

            public OperationInfoBuilder description(String description) {
                info.description = description;
                return this;
            }

            public OperationInfoBuilder inputType(Class<?> inputType) {
                info.inputType = inputType;
                return this;
            }

            public OperationInfoBuilder outputType(Class<?> outputType) {
                info.outputType = outputType;
                return this;
            }

            public OperationInfoBuilder enabled(boolean enabled) {
                info.enabled = enabled;
                return this;
            }

            public OperationInfoBuilder supportedModels(String[] supportedModels) {
                info.supportedModels = supportedModels;
                return this;
            }

            public OperationInfoBuilder defaultModel(String defaultModel) {
                info.defaultModel = defaultModel;
                return this;
            }

            public OperationInfo build() {
                return info;
            }
        }
    }

    /**
     * 执行统计类
     */
    public static class ExecutionStats {
        private long totalExecutions = 0;
        @Getter
        private long successfulExecutions = 0;
        @Getter
        private long failedExecutions = 0;
        @Getter
        private long totalDuration = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;

        public synchronized void record(boolean success, long duration) {
            totalExecutions++;
            if (success) {
                successfulExecutions++;
            } else {
                failedExecutions++;
            }

            totalDuration += duration;
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
        }

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        }

        public double getAverageDuration() {
            return totalExecutions > 0 ? (double) totalDuration / totalExecutions : 0.0;
        }

        public long getMinDuration() { return minDuration == Long.MAX_VALUE ? 0 : minDuration; }
    }
}