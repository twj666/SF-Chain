package com.suifeng.sfchain.core;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.logging.AICallLog;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.openai.OpenAICompatibleModel;
import com.suifeng.sfchain.persistence.context.ChatContextService;
import com.suifeng.sfchain.persistence.context.ChatMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.suifeng.sfchain.constants.AIOperationConstant.JSON_REPAIR_OP;

/**
 * 描述: AI操作抽象基类 - 新框架版本
 * 提供统一的AI操作接口和实现
 *
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
public abstract class BaseAIOperation<INPUT, OUTPUT> {

    @Autowired
    protected AIOperationRegistry operationRegistry;

    @Autowired
    protected ModelRegistry modelRegistry;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ChatContextService chatContextService;

    /**
     * 操作的注解信息
     * -- GETTER --
     * 获取注解信息
     *
     * @return 注解信息
     */
    @Getter
    private AIOp annotation;

    /**
     * 输入类型
     * -- GETTER --
     * 获取输入类型
     *
     * @return 输入类型
     */
    @Getter
    private Class<INPUT> inputType;

    /**
     * 输出类型
     * -- GETTER --
     * 获取输出类型
     *
     * @return 输出类型
     */
    @Getter
    private Class<OUTPUT> outputType;

    /**
     * 初始化方法
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        // 获取注解信息
        this.annotation = this.getClass().getAnnotation(AIOp.class);
        if (annotation == null) {
            throw new IllegalStateException("AI操作类必须使用@AIOp注解: " + this.getClass().getSimpleName());
        }

        // 获取泛型类型
        Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length >= 2) {
                this.inputType = (Class<INPUT>) typeArguments[0];
                this.outputType = (Class<OUTPUT>) typeArguments[1];
            }
        }

        // 注册到操作注册中心
        operationRegistry.registerOperation(annotation.value(), this);

        // 如果注解中有默认模型且当前没有设置模型映射，则自动设置
        if (!annotation.defaultModel().isEmpty()) {
            String currentModel = operationRegistry.getModelForOperation(annotation.value());
            if (currentModel == null) {
                try {
                    // 验证模型是否存在
                    if (modelRegistry.getModel(annotation.defaultModel()) != null) {
                        operationRegistry.setModelForOperation(annotation.value(), annotation.defaultModel());
                        log.info("自动设置操作默认模型映射: {} -> {}", annotation.value(), annotation.defaultModel());
                    }
                } catch (Exception e) {
                    log.warn("无法设置默认模型映射 {} -> {}: {}", annotation.value(), annotation.defaultModel(), e.getMessage());
                }
            }
        }

        log.info("初始化AI操作: {} [{}] -> 输入类型: {}, 输出类型: {}",
                annotation.value(), this.getClass().getSimpleName(),
                inputType != null ? inputType.getSimpleName() : "Unknown",
                outputType != null ? outputType.getSimpleName() : "Unknown");
    }

    /**
     * 流式执行AI操作
     */
    @SuppressWarnings("unchecked")
    public Flux<String> executeStream(INPUT input) {
        return executeStream(input, null, null);
    }

    /**
     * 流式执行AI操作（指定模型）
     */
    @SuppressWarnings("unchecked")
    public Flux<String> executeStream(INPUT input, String modelName) {
        return executeStream(input, modelName, null);
    }

    /**
     * 流式执行AI操作（带上下文支持）
     */
    @SuppressWarnings("unchecked")
    public Flux<String> executeStream(INPUT input, String modelName, String sessionId) {
        try {
            // 检查操作是否启用
            if (!isEnabled()) {
                return Flux.error(new IllegalStateException("操作已禁用: " + annotation.value()));
            }

            // 获取模型
            AIModel model = getModel(modelName);
            
            // 构建带上下文的提示词
            String prompt = buildPromptWithContext(input, sessionId);
            
            // 获取操作配置
            AIOperationRegistry.OperationConfig config = operationRegistry.getOperationConfig(annotation.value());
            
            // 合并配置
            Integer finalMaxTokens = config.getMaxTokens() > 0 ? Integer.valueOf(config.getMaxTokens()) : 
                                    (annotation.defaultMaxTokens() > 0 ? annotation.defaultMaxTokens() : null);
            Double finalTemperature = config.getTemperature() >= 0 ? Double.valueOf(config.getTemperature()) : 
                                     (annotation.defaultTemperature() >= 0 ? annotation.defaultTemperature() : null);
            Boolean finalJsonOutput = config.isRequireJsonOutput() || annotation.requireJsonOutput();
            boolean finalThinking = config.isSupportThinking() || annotation.supportThinking();
            
            // 调用模型的流式生成方法
            if (model instanceof OpenAICompatibleModel openAIModel) {
                if (finalThinking) {
                    return openAIModel.generateStreamWithThinking(prompt, finalMaxTokens, finalTemperature);
                } else {
                    return openAIModel.generateStream(prompt, finalMaxTokens, finalTemperature, finalJsonOutput);
                }
            } else {
                // 对于不支持流式的模型，返回错误
                return Flux.error(new UnsupportedOperationException("模型不支持流式输出: " + model.getName()));
            }
            
        } catch (Exception e) {
            log.error("流式AI操作执行失败: {} - {}", annotation.value(), e.getMessage(), e);
            return Flux.error(new RuntimeException("流式AI操作执行失败: " + e.getMessage(), e));
        }
    }

    /**
     * 执行AI操作
     *
     * @param input 输入参数
     * @return 输出结果
     */
    public OUTPUT execute(INPUT input) {
        return execute(input, null, null);
    }

    /**
     * 执行AI操作（指定模型）
     *
     * @param input     输入参数
     * @param modelName 指定的模型名称，为null时使用默认模型
     * @return 输出结果
     */
    public OUTPUT execute(INPUT input, String modelName) {
        return execute(input, modelName, null);
    }

    /**
     * 执行AI操作（带上下文支持）
     *
     * @param input 输入参数
     * @param modelName 指定的模型名称，为null时使用默认模型
     * @param sessionId 会话ID，用于上下文管理
     * @return 输出结果
     */
    // 在BaseAIOperation类中添加以下字段和方法

    @Autowired
    private AICallLogManager logManager;

    // 在execute方法中添加详细日志记录和上下文支持
    public OUTPUT execute(INPUT input, String modelName, String sessionId) {
        String callId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();

        AICallLog.AICallLogBuilder logBuilder = AICallLog.builder()
                .callId(callId)
                .operationType(annotation.value())
                .callTime(startTime)
                .input(input)
                .modelName(modelName)
                .frequency(1)
                .lastAccessTime(startTime);

        try {
            // 获取模型
            AIModel model = getModel(modelName);
            logBuilder.modelName(model.getName());

            // 构建提示词（带上下文支持）
            String prompt = buildPromptWithContext(input, sessionId);
            logBuilder.prompt(prompt);

            // 获取操作配置
            AIOperationRegistry.OperationConfig config = operationRegistry.getOperationConfig(annotation.value());

            // 合并配置
            Integer finalMaxTokens = config.getMaxTokens() > 0 ? Integer.valueOf(config.getMaxTokens()) : (annotation.defaultMaxTokens() > 0 ? annotation.defaultMaxTokens() : null);
            Double finalTemperature = config.getTemperature() >= 0 ? Double.valueOf(config.getTemperature()) : (annotation.defaultTemperature() >= 0 ? annotation.defaultTemperature() : null);
            Boolean finalJsonOutput = config.isRequireJsonOutput() || annotation.requireJsonOutput();
            boolean finalThinking = config.isSupportThinking() || annotation.supportThinking();

            // 记录请求参数
            AICallLog.AIRequestParams requestParams = AICallLog.AIRequestParams.builder()
                    .maxTokens(finalMaxTokens)
                    .temperature(finalTemperature)
                    .jsonOutput(finalJsonOutput)
                    .thinking(finalThinking)
                    .build();
            logBuilder.requestParams(requestParams);

            // 调用AI模型
            String response;
            if (model instanceof OpenAICompatibleModel openAIModel) {
                if (finalThinking) {
                    response = openAIModel.generateWithThinking(prompt, finalMaxTokens, finalTemperature);
                } else {
                    response = openAIModel.generate(prompt, finalMaxTokens, finalTemperature, finalJsonOutput);
                }
            } else {
                response = model.generate(prompt);
            }

            logBuilder.rawResponse(response);

            // 解析响应
            OUTPUT result = parseResponse(response, input);

            // 记录成功日志
            long duration = System.currentTimeMillis() - startMillis;
            AICallLog log = logBuilder
                    .status(AICallLog.CallStatus.SUCCESS)
                    .duration(duration)
                    .output(result)
                    .build();

            logManager.addLog(log);

            return result;

        } catch (Exception e) {
            // 记录失败日志
            long duration = System.currentTimeMillis() - startMillis;
            AICallLog callLog = logBuilder
                    .status(AICallLog.CallStatus.FAILED)
                    .duration(duration)
                    .errorMessage(e.getMessage())
                    .build();

            logManager.addLog(callLog);

            log.error("执行AI操作失败: {} - {}", annotation.value(), e.getMessage(), e);
            throw new RuntimeException("AI操作执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建带上下文的提示词
     *
     * @param input     输入参数
     * @param sessionId 会话ID
     * @return 完整的提示词
     */
    protected String buildPromptWithContext(INPUT input, String sessionId) {
        // 构建基础提示词
        String basePrompt = buildPrompt(input);

        // 如果没有会话ID，直接返回基础提示词
        if (sessionId == null || !chatContextService.sessionExists(sessionId)) {
            return basePrompt;
        }

        // 获取上下文信息
        List<ChatMessage> contextMessages = chatContextService.getFullContext(sessionId);
        if (contextMessages.isEmpty()) {
            return basePrompt;
        }

        // 构建带上下文的提示词
        StringBuilder contextPrompt = new StringBuilder();

        // 添加系统提示词（如果存在）
        String systemPrompt = chatContextService.getSystemPrompt(sessionId);
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            contextPrompt.append("系统提示: ").append(systemPrompt).append("\n\n");
        }

        // 添加对话历史
        List<ChatMessage> conversationHistory = chatContextService.getConversationHistory(sessionId);
        if (!conversationHistory.isEmpty()) {
            contextPrompt.append("对话历史:\n");
            for (ChatMessage message : conversationHistory) {
                String role = getRoleString(message.getType());
                contextPrompt.append(role).append(": ").append(message.getContent()).append("\n");
            }
            contextPrompt.append("\n");
        }

        // 添加当前任务提示词
        contextPrompt.append("当前任务:\n").append(basePrompt);

        return contextPrompt.toString();
    }

    /**
     * 获取角色字符串
     *
     * @param type 消息类型
     * @return 角色字符串
     */
    private String getRoleString(ChatMessage.MessageType type) {
        switch (type) {
            case SYSTEM:
                return "系统";
            case USER:
                return "用户";
            case ASSISTANT:
                return "助手";
            default:
                return "未知";
        }
    }

    /**
     * 构建提示词（子类实现）
     *
     * @param input 输入参数
     * @return 提示词
     */
    protected abstract String buildPrompt(INPUT input);

    /**
     * 解析AI响应（最终方法，子类不应重写）
     * 重写版本：支持非JSON格式输出
     *
     * @param response AI响应
     * @param input    输入参数
     * @return 解析后的结果
     */
    protected final OUTPUT parseResponse(String response, INPUT input) {
        // 如果输出类型是String，直接返回响应内容
        if (outputType == String.class) {
            // 先尝试子类自定义解析
            OUTPUT customResult = parseResult(response, input);
            if (customResult != null) {
                return customResult;
            }
            // 默认返回原始响应
            return (OUTPUT) response;
        }

        // 如果不要求JSON输出，也尝试直接解析
        if (!annotation.requireJsonOutput()) {
            OUTPUT customResult = parseResult(response, input);
            if (customResult != null) {
                return customResult;
            }
            // 如果子类没有自定义解析且不要求JSON，返回原始响应（如果类型兼容）
            if (outputType.isAssignableFrom(String.class)) {
                return (OUTPUT) response;
            }
        }

        // 以下是原有的JSON解析逻辑
        try {
            // 1. 预处理响应（子类可自定义）
            String processedResponse = preprocessResponse(response, input);

            // 2. 提取JSON内容
            String jsonContent = extractJsonFromResponse(processedResponse);

            // 3. 预处理JSON内容（子类可自定义）
            String processedJson = preprocessJson(jsonContent, input);

            // 4. 解析为对象（子类可自定义解析逻辑）
            return parseJsonToResult(processedJson, input, response);

        } catch (JsonProcessingException e) {
            // 如果启用了自动JSON修复且需要JSON输出，尝试修复JSON
            if (annotation.requireJsonOutput() && annotation.autoRepairJson()) {
                log.warn("JSON解析失败，尝试自动修复: {}", e.getMessage());
                try {
                    // 通过操作注册中心获取JSON修复操作，避免循环依赖
                    BaseAIOperation<?, ?> jsonRepairOp = operationRegistry.getOperation(JSON_REPAIR_OP);
                    if (jsonRepairOp != null) {
                        String jsonContent = extractJsonFromResponse(response);
                        @SuppressWarnings("unchecked")
                        BaseAIOperation<String, JSONObject> repairOperation = (BaseAIOperation<String, JSONObject>) jsonRepairOp;
                        JSONObject repairedJson = repairOperation.execute(jsonContent);
                        String repairedJsonStr = repairedJson.toJSONString();
                        return parseJsonToResult(repairedJsonStr, input, response);
                    }
                } catch (Exception repairException) {
                    log.error("JSON修复也失败: {}", repairException.getMessage(), repairException);
                    throw new RuntimeException("JSON解析和修复都失败: 原始错误=" + e.getMessage() + ", 修复错误=" + repairException.getMessage(), e);
                }
            }

            log.error("解析AI响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 预处理AI响应（子类可重写）
     * 在提取JSON之前对原始响应进行处理
     *
     * @param response 原始AI响应
     * @param input    输入参数
     * @return 处理后的响应
     */
    protected String preprocessResponse(String response, INPUT input) {
        return response;
    }

    /**
     * 预处理JSON内容（子类可重写）
     * 在JSON解析之前对提取的JSON字符串进行处理
     *
     * @param jsonContent 提取的JSON字符串
     * @param input       输入参数
     * @return 处理后的JSON字符串
     */
    protected String preprocessJson(String jsonContent, INPUT input) {
        return jsonContent;
    }

    /**
     * 将JSON字符串解析为结果对象（高级用法，一般用户无需重写）
     *
     * @param jsonContent      JSON内容
     * @param input            输入参数
     * @param originalResponse 原始响应
     * @return 解析后的结果对象
     * @throws JsonProcessingException JSON解析异常
     */
    protected OUTPUT parseJsonToResult(String jsonContent, INPUT input, String originalResponse) throws JsonProcessingException {
        // 先尝试用户自定义的解析方法
        OUTPUT customResult = parseResult(jsonContent, input);
        if (customResult != null) {
            return customResult;
        }

        // 如果用户没有自定义解析，使用默认的JSON解析
        return objectMapper.readValue(jsonContent, outputType);
    }

    /**
     * 解析AI返回的JSON为最终结果（推荐用户重写此方法）
     * 用户可以在此方法中处理AI返回的原始JSON，并转换为最终的结果对象
     *
     * @param jsonContent AI返回的JSON字符串
     * @param input       输入参数
     * @return 最终结果对象，如果返回null则使用默认的JSON解析
     */
    protected OUTPUT parseResult(String jsonContent, INPUT input) {
        return null; // 默认返回null，表示使用框架的默认JSON解析
    }

    /**
     * 工具方法：将JSON字符串解析为指定类型的对象
     *
     * @param jsonContent JSON字符串
     * @param clazz       目标类型
     * @param <T>         泛型类型
     * @return 解析后的对象
     * @throws JsonProcessingException JSON解析异常
     */
    protected <T> T parseJsonToObject(String jsonContent, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(jsonContent, clazz);
    }

    /**
     * 从响应中提取JSON内容
     *
     * @param response 原始响应
     * @return JSON字符串
     */
    protected String extractJsonFromResponse(String response) {
        // 查找JSON代码块
        String jsonStart = "```json";
        String jsonEnd = "```";

        int startIndex = response.indexOf(jsonStart);
        if (startIndex != -1) {
            startIndex += jsonStart.length();
            int endIndex = response.indexOf(jsonEnd, startIndex);
            if (endIndex != -1) {
                return response.substring(startIndex, endIndex).trim();
            }
        }

        // 查找花括号包围的JSON
        int braceStart = response.indexOf('{');
        int braceEnd = response.lastIndexOf('}');
        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            return response.substring(braceStart, braceEnd + 1);
        }

        // 如果都找不到，返回原始响应
        return response;
    }

    /**
     * 获取模型实例
     *
     * @param modelName 模型名称，为null时使用默认模型
     * @return 模型实例
     */
    private AIModel getModel(String modelName) {
        if (modelName == null) {
            // 使用注册中心配置的模型
            modelName = operationRegistry.getModelForOperation(annotation.value());
        }

        if (modelName == null) {
            // 使用注解中的默认模型
            modelName = annotation.defaultModel();
        }

        if (modelName == null || modelName.isEmpty()) {
            throw new IllegalStateException("未配置模型: " + annotation.value());
        }

        AIModel model = modelRegistry.getModel(modelName);
        if (model == null) {
            throw new IllegalArgumentException("模型不存在: " + modelName);
        }

        return model;
    }

    /**
     * 获取操作类型
     *
     * @return 操作类型
     */
    public String getOperationType() {
        return annotation != null ? annotation.value() : null;
    }

    /**
     * 检查操作是否启用
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        AIOperationRegistry.OperationConfig config = operationRegistry.getOperationConfig(annotation.value());
        return config.isEnabled() && annotation.enabled();
    }

    /**
     * 获取操作描述
     *
     * @return 操作描述
     */
    public String getDescription() {
        return annotation.description();
    }

    /**
     * 获取支持的模型列表
     *
     * @return 支持的模型列表
     */
    public String[] getSupportedModels() {
        return annotation.supportedModels();
    }

}
