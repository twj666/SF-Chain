package com.suifeng.sfchain.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.BaseAIOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.suifeng.sfchain.constants.AIOperationConstant.MODEL_VALIDATION_OP;

/**
 * 描述: 模型验证操作 - 用于验证模型配置是否可用
 *
 * @author suifeng
 * 日期: 2025/1/27
 */
@Slf4j
@Component
@AIOp(
        value = MODEL_VALIDATION_OP,
        description = "验证模型配置是否可用的简单测试操作",
        requireJsonOutput = false
)
public class ModelValidationOperation extends BaseAIOperation<ModelValidationOperation.ValidationRequest, ModelValidationOperation.ValidationResult> {

    public static final String FALLBACK_ANSWER = "模型响应解析失败，但模型可以正常通信";
    private static final String TEMPLATE = """
            请回答一个简单的问题来验证模型是否正常工作。
            问题: {{ input.question }}
            请严格作答，并以JSON格式返回结果，只需要返回以下json串：
            ```json
            {
              "answer": "5"
            }
            ```
            注意：请确保返回有效的JSON格式。""";

    @Override
    public String promptTemplate() {
        return TEMPLATE;
    }

    public static String buildValidationPrompt(ValidationRequest request) {
        ValidationRequest safeRequest = request == null ? new ValidationRequest() : request;
        String question = safeRequest.getQuestion() == null ? "" : safeRequest.getQuestion();
        return TEMPLATE.replace("{{ input.question }}", question);
    }

    public static ValidationResult parseValidationResult(ObjectMapper objectMapper, String rawContent) {
        String normalized = extractJsonPayload(rawContent);
        try {
            return objectMapper.readValue(normalized, ValidationResult.class);
        } catch (Exception e) {
            ValidationResult result = new ValidationResult();
            result.setAnswer(FALLBACK_ANSWER);
            return result;
        }
    }

    private static String extractJsonPayload(String rawContent) {
        if (rawContent == null) {
            return "";
        }
        String text = rawContent.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd > -1 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    @Override
    protected ValidationResult parseResult(String jsonContent, ValidationRequest input) {
        ValidationResult result = parseValidationResult(objectMapper, jsonContent);
        if (FALLBACK_ANSWER.equals(result.getAnswer())) {
            log.warn("解析验证响应失败，使用默认结果");
        }
        return result;
    }

    @Override
    public String getDescription() {
        return "模型验证操作 - 通过简单问答验证模型配置是否可用";
    }

    /**
     * 验证请求
     */
    @Data
    public static class ValidationRequest {
        @JsonProperty("question")
        private String question;

        public ValidationRequest() {
            this.question = "1+1等于几？";
        }

        public ValidationRequest(String question) {
            this.question = question;
        }
    }

    /**
     * 验证结果
     */
    @Data
    public static class ValidationResult {
        @JsonProperty("answer")
        private String answer;
    }
}
