package com.sfchain.models.qwen;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
import com.sfchain.core.model.AbstractAIModel;
import com.sfchain.core.model.ModelParameters;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 描述: 千问Plus模型实现
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class QwenPlusModel extends AbstractAIModel {

    public static final String MODEL_NAME = "qwen-plus";

    /**
     * 构造函数
     *
     * @param config 模型配置
     */
    public QwenPlusModel(QwenConfig config) {
        super(config);
    }

    @Override
    public String getName() {
        return MODEL_NAME;
    }

    @Override
    public String description() {
        return "通义千问 (plus版本)";
    }

    @Override
    protected String doGenerate(String prompt) throws Exception {
        QwenConfig config = (QwenConfig) this.config;
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
            throw new RuntimeException("Failed to parse Qwen response: " + e.getMessage(), e);
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
        QwenConfig config = (QwenConfig) this.config;
        URL url = new URL(config.getBaseUrl() + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
        conn.setDoOutput(true);

        ModelParameters params = getParameters();
        String systemPrompt = params.systemPrompt() != null ? params.systemPrompt() : config.getSystemPrompt();

        RequestBody requestBody = new RequestBody(
                config.getVersion(),
                new Message[] {
                        new Message("system", systemPrompt),
                        new Message("user", content)
                },
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
}