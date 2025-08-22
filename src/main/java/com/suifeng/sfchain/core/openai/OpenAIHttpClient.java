package com.suifeng.sfchain.core.openai;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 描述: OpenAI兼容的HTTP客户端
 * @author suifeng
 * 日期: 2025/8/11
 */
@Slf4j
public class OpenAIHttpClient {
    
    private final String baseUrl;
    private final String apiKey;
    private final Map<String, String> defaultHeaders;
    
    public OpenAIHttpClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.defaultHeaders = Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer " + apiKey
        );
    }
    
    public OpenAIHttpClient(String baseUrl, String apiKey, Map<String, String> additionalHeaders) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.defaultHeaders = new HashMap<>();
        this.defaultHeaders.put("Content-Type", "application/json");
        this.defaultHeaders.put("Authorization", "Bearer " + apiKey);
        if (additionalHeaders != null) {
            this.defaultHeaders.putAll(additionalHeaders);
        }
    }
    
    /**
     * 发送聊天完成请求
     */
    public OpenAIResponse chatCompletion(OpenAIRequest request) {
        try {
            // 智能构建endpoint，避免重复的/v1路径
            String endpoint;
            if (baseUrl.endsWith("/v1") || baseUrl.contains("/v1/")) {
                // baseUrl已包含v1路径，直接添加chat/completions
                endpoint = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "chat/completions";
            } else {
                // baseUrl不包含v1路径，添加完整路径
                endpoint = baseUrl + "/v1/chat/completions";
            }
            String requestBody = JSON.toJSONString(request);
            
            log.debug("发送请求到: {}", endpoint);
            log.debug("请求体: {}", requestBody);
            log.info("构建的API端点: {}", endpoint);
            
             HttpURLConnection connection = createConnection(endpoint);
            
            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取响应
            StringBuilder response = new StringBuilder();
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
            } else {
                // 读取错误响应
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                throw new RuntimeException("HTTP请求失败，状态码: " + responseCode + ", 响应: " + response.toString());
            }

            String responseBody = response.toString();
            log.debug("响应体: {}", responseBody);
            
            return JSON.parseObject(responseBody, OpenAIResponse.class);
            
        } catch (Exception e) {
            log.error("OpenAI API调用失败", e);
            throw new RuntimeException("OpenAI API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建HTTP连接
     */
    private HttpURLConnection createConnection(String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000); // 30秒连接超时
        connection.setReadTimeout(120000);   // 120秒读取超时
        
        // 设置请求头
        defaultHeaders.forEach(connection::setRequestProperty);
        
        return connection;
    }
    
    /**
     * 提取响应内容
     */
    public String extractContent(OpenAIResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        
        OpenAIResponse.Choice choice = response.getChoices().get(0);
        if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
            return choice.getMessage().getContent();
        }
        
        return "";
    }
    
    /**
     * 发送流式聊天完成请求
     */
    public Flux<String> chatCompletionStream(OpenAIRequest request) {
        return Flux.create(sink -> {
            // 在新线程中异步处理流式响应
            CompletableFuture.runAsync(() -> {
                try {
                    // 设置流式请求
                    OpenAIRequest streamRequest = request.toBuilder().stream(true).build();
                    
                    // 智能构建endpoint
                    String endpoint;
                    if (baseUrl.endsWith("/v1") || baseUrl.contains("/v1/")) {
                        endpoint = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "chat/completions";
                    } else {
                        endpoint = baseUrl + "/v1/chat/completions";
                    }
                    
                    String requestBody = JSON.toJSONString(streamRequest);
                    log.debug("发送流式请求到: {}", endpoint);
                    log.debug("请求体: {}", requestBody);
                    
                    HttpURLConnection connection = createConnection(endpoint);
                    
                    // 发送请求体
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                    
                    // 读取流式响应
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            
                            String line;
                            while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                                if (line.trim().isEmpty()) {
                                    continue;
                                }
                                
                                // 处理SSE格式的数据
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    
                                    // 检查是否为结束标记
                                    if ("[DONE]".equals(data)) {
                                        sink.complete();
                                        break;
                                    }
                                    
                                    try {
                                        // 解析流式响应
                                        OpenAIStreamResponse streamResponse = JSON.parseObject(data, OpenAIStreamResponse.class);
                                        String content = extractStreamContent(streamResponse);
                                        if (content != null && !content.isEmpty()) {
                                            // 立即发送内容，而不是缓存
                                            sink.next(content);
                                            // 添加小延迟以确保流式效果
                                            Thread.sleep(10);
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析流式响应失败: {}", data, e);
                                    }
                                }
                            }
                            
                            if (!sink.isCancelled()) {
                                sink.complete();
                            }
                        }
                    } else {
                        // 读取错误响应
                        StringBuilder errorResponse = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                errorResponse.append(line);
                            }
                        }
                        sink.error(new RuntimeException("HTTP请求失败，状态码: " + responseCode + ", 响应: " + errorResponse.toString()));
                    }
                    
                } catch (Exception e) {
                    log.error("流式OpenAI API调用失败", e);
                    sink.error(new RuntimeException("流式OpenAI API调用失败: " + e.getMessage(), e));
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }
    
    /**
     * 提取流式响应内容
     */
    private String extractStreamContent(OpenAIStreamResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        
        OpenAIStreamResponse.StreamChoice choice = response.getChoices().get(0);
        if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
            return choice.getDelta().getContent();
        }
        
        return null;
    }
}