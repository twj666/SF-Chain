package com.suifeng.sfchain.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Override
    protected String buildPrompt(ValidationRequest input) {
        return String.format(
                """
                        请回答一个简单的问题来验证模型是否正常工作。
                        问题: %s
                        请严格作答，并以JSON格式返回结果：
                        ```json
                        {
                          "answer": "5"
                        }
                        ```
                        注意：请确保返回有效的JSON格式。""",
                input.getQuestion()
        );
    }

    @Override
    protected ValidationResult parseResult(String jsonContent, ValidationRequest input) {
        try {
            return objectMapper.readValue(jsonContent, ValidationResult.class);
        } catch (Exception e) {
            log.warn("解析验证响应失败，使用默认结果: {}", e.getMessage());
            ValidationResult result = new ValidationResult();
            result.setAnswer("模型响应解析失败，但模型可以正常通信");
            return result;
        }
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