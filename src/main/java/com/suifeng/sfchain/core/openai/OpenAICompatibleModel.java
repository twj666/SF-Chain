package com.suifeng.sfchain.core.openai;

import com.alibaba.fastjson2.JSON;
import com.suifeng.sfchain.core.AIModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 描述: OpenAI兼容的通用模型实现
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
public class OpenAICompatibleModel implements AIModel {

    /**
     * -- GETTER --
     *  获取模型配置
     */
    @Getter
    private final OpenAIModelConfig config;
    private final OpenAIHttpClient httpClient;
    
    public OpenAICompatibleModel(OpenAIModelConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("模型配置无效: " + config);
        }
        
        this.config = config;
        this.httpClient = new OpenAIHttpClient(
            config.getBaseUrl(), 
            config.getApiKey(), 
            config.getAdditionalHeaders()
        );
        
        log.info("初始化OpenAI兼容模型: {} ({})", config.getModelName(), config.getProvider());
    }
    
    @Override
    public String getName() {
        return config.getModelName();
    }
    
    @Override
    public String description() {
        return config.getDescription() != null ? config.getDescription() : 
               String.format("%s模型 (提供商: %s)", config.getModelName(), config.getProvider());
    }
    
    @Override
    public String generate(String prompt) {
        return generate(prompt, null, null, null);
    }
    
    @Override
    public <T> T generate(String prompt, Class<T> responseType) {
        String result = generate(prompt);
        if (responseType == String.class) {
            return responseType.cast(result);
        }
        
        try {
            return JSON.parseObject(result, responseType);
        } catch (Exception e) {
            log.error("解析响应为{}类型失败: {}", responseType.getSimpleName(), e.getMessage());
            throw new RuntimeException("响应解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成响应 - 支持自定义参数
     */
    public String generate(String prompt, Integer maxTokens, Double temperature, Boolean jsonOutput) {
        try {
            OpenAIRequest request = buildRequest(prompt, maxTokens, temperature, jsonOutput);
            OpenAIResponse response = httpClient.chatCompletion(request);
            return httpClient.extractContent(response);
        } catch (Exception e) {
            log.error("模型{}生成失败", config.getModelName(), e);
            throw new RuntimeException("模型生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成响应 - 支持思考模式
     */
    public String generateWithThinking(String prompt, Integer maxTokens, Double temperature) {
        if (!Boolean.TRUE.equals(config.getSupportThinking())) {
            log.warn("模型{}不支持思考模式，使用普通模式", config.getModelName());
            return generate(prompt, maxTokens, temperature, null);
        }
        
        try {
            OpenAIRequest request = buildRequestWithThinking(prompt, maxTokens, temperature);
            OpenAIResponse response = httpClient.chatCompletion(request);
            return httpClient.extractContent(response);
        } catch (Exception e) {
            log.error("模型{}思考模式生成失败", config.getModelName(), e);
            throw new RuntimeException("思考模式生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建请求对象
     */
    private OpenAIRequest buildRequest(String prompt, Integer maxTokens, Double temperature, Boolean jsonOutput) {
        var builder = OpenAIRequest.builder()
            .model(config.getModelName())
            .messages(List.of(
                OpenAIRequest.Message.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            ))
            .max_tokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
            .temperature(temperature != null ? temperature : config.getDefaultTemperature())
            .stream(false);
        
        // 设置JSON输出格式
        if (Boolean.TRUE.equals(jsonOutput) && Boolean.TRUE.equals(config.getSupportJsonOutput())) {
            builder.response_format(Map.of("type", "json_object"));
        }
        
        return builder.build();
    }
    
    /**
     * 构建带思考模式的请求对象
     */
    private OpenAIRequest buildRequestWithThinking(String prompt, Integer maxTokens, Double temperature) {
        return OpenAIRequest.builder()
            .model(config.getModelName())
            .messages(List.of(
                OpenAIRequest.Message.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            ))
            .max_tokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
            .temperature(temperature != null ? temperature : config.getDefaultTemperature())
            .stream(false)
            .enable_thinking(true)
            .build();
    }

    /**
     * 检查模型是否可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(config.getEnabled()) && config.isValid();
    }
    
    /**
     * 流式生成响应
     */
    public Flux<String> generateStream(String prompt) {
        return generateStream(prompt, null, null, null);
    }
    
    /**
     * 流式生成响应 - 支持自定义参数
     */
    public Flux<String> generateStream(String prompt, Integer maxTokens, Double temperature, Boolean jsonOutput) {
        try {
            OpenAIRequest request = buildStreamRequest(prompt, maxTokens, temperature, jsonOutput);
            return httpClient.chatCompletionStream(request);
        } catch (Exception e) {
            log.error("模型{}流式生成失败", config.getModelName(), e);
            return Flux.error(new RuntimeException("模型流式生成失败: " + e.getMessage(), e));
        }
    }
    
    /**
     * 流式生成响应 - 支持思考模式
     */
    public Flux<String> generateStreamWithThinking(String prompt, Integer maxTokens, Double temperature) {
        if (!Boolean.TRUE.equals(config.getSupportThinking())) {
            log.warn("模型{}不支持思考模式，使用普通流式模式", config.getModelName());
            return generateStream(prompt, maxTokens, temperature, null);
        }
        
        try {
            OpenAIRequest request = buildStreamRequestWithThinking(prompt, maxTokens, temperature);
            return httpClient.chatCompletionStream(request);
        } catch (Exception e) {
            log.error("模型{}思考模式流式生成失败", config.getModelName(), e);
            return Flux.error(new RuntimeException("思考模式流式生成失败: " + e.getMessage(), e));
        }
    }

    /**
     * 构建流式请求对象
     */
    private OpenAIRequest buildStreamRequest(String prompt, Integer maxTokens, Double temperature, Boolean jsonOutput) {
        var builder = OpenAIRequest.builder()
            .model(config.getModelName())
            .messages(List.of(
                OpenAIRequest.Message.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            ))
            .max_tokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
            .temperature(temperature != null ? temperature : config.getDefaultTemperature())
            .stream(true);  // 设置为true以启用流式输出
        
        // 设置JSON输出格式
        if (Boolean.TRUE.equals(jsonOutput) && Boolean.TRUE.equals(config.getSupportJsonOutput())) {
            builder.response_format(Map.of("type", "json_object"));
        }
        
        return builder.build();
    }
    
    /**
     * 构建带思考模式的流式请求对象
     */
    private OpenAIRequest buildStreamRequestWithThinking(String prompt, Integer maxTokens, Double temperature) {
        return OpenAIRequest.builder()
            .model(config.getModelName())
            .messages(List.of(
                OpenAIRequest.Message.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            ))
            .max_tokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
            .temperature(temperature != null ? temperature : config.getDefaultTemperature())
            .stream(true)
            .enable_thinking(true)
            .build();
    }
}