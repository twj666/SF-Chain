package com.sfchain.models.flow;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
import com.sfchain.core.model.AbstractAIModel;
import com.sfchain.core.model.ModelParameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 描述: SiliconFlow模型抽象基类
 * @author suifeng
 * 日期: 2025/4/15
 */
public abstract class AbstractSiliconFlowModel extends AbstractAIModel {
    
    /**
     * 构造函数
     * 
     * @param config 模型配置
     */
    public AbstractSiliconFlowModel(SiliconflowConfig config) {
        super(config);
    }
    
    @Override
    protected String doGenerate(String prompt) throws Exception {
        SiliconflowConfig config = (SiliconflowConfig) this.config;
        HttpURLConnection connection = createConnection(prompt);
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        JSONObject jsonResponse = JSONObject.parseObject(response.toString());
        return extractContent(jsonResponse);
    }
    
    @Override
    public <T> T generate(String prompt, Class<T> responseType) {
        String response = generate(prompt);
        try {
            return new Gson().fromJson(response, responseType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SiliconFlow response: " + e.getMessage(), e);
        }
    }
    
    private String extractContent(JSONObject jsonResponse) {
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            return message.getString("content");
        }
        return "";
    }
    
    private HttpURLConnection createConnection(String content) throws Exception {
        SiliconflowConfig config = (SiliconflowConfig) this.config;
        URL url = new URL(config.getBaseUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
        conn.setDoOutput(true);
        
        ModelParameters params = getParameters();
        RequestBody requestBody = new RequestBody(
            getModelVersion(),
            new Message[] { new Message("user", content) },
            params.maxTokens(),
            params.temperature()
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = new Gson().toJson(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }
    
    // 内部数据结构
    @lombok.Data
    private static class RequestBody {
        private final String model;
        private final Message[] messages;
        private final Integer max_tokens;
        private final Double temperature;
        private final boolean stream = false;
    }
    
    @lombok.Data
    private static class Message {
        private final String role;
        private final String content;
    }
    
    /**
     * 获取模型版本
     * 
     * @return 模型版本
     */
    protected abstract String getModelVersion();
}