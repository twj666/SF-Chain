package com.sfchain.operations.common.sample1;

import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.exception.OperationException;
import com.sfchain.core.operation.BaseAIOperation;
import com.sfchain.core.operation.AIPromptBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 描述: 文本生成示例
 * @author suifeng
 * 日期: 2025/4/15
 */
@AIOp("text-generation")
@Component
public class TextGenerationOperation extends BaseAIOperation<Map<String, Object>, String> {

    private static final String DEFAULT_ROLE = "You are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe.";

    @Override
    public List<String> supportedModels() {
        return List.of("deepseek-chat", "qwen-plus", "gpt-4o", "gpt-3.5-turbo", "claude-3-opus");
    }

    @Override
    public String buildPrompt(Map<String, Object> params) {
        // 验证必要参数
        if (!params.containsKey("prompt")) {
            throw new OperationException("text-generation", "Missing required parameter: prompt");
        }

        String prompt = getStringValue(params, "prompt", "");
        String systemPrompt = getStringValue(params, "systemPrompt", DEFAULT_ROLE);
        Boolean isMarkdown = getBooleanValue(params, "markdown", true);

        // 创建提示词构建器
        AIPromptBuilder builder = createPromptBuilder("Text Generation");

        // 添加系统角色描述
        builder.addRole(systemPrompt);

        // 处理会话历史
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) params.getOrDefault("history", List.of());

        if (!history.isEmpty()) {
            // 移除最后一条用户消息，因为它将作为当前提示词
            if (history.size() >= 2 && "user".equals(history.get(history.size() - 1).get("role"))) {
                history = history.subList(0, history.size() - 1);
            }

            // 添加历史对话
            if (!history.isEmpty()) {
                StringBuilder historyText = new StringBuilder();
                for (Map<String, String> message : history) {
                    String role = message.get("role");
                    String content = message.get("content");

                    if ("user".equals(role)) {
                        historyText.append("User: ").append(content).append("\n\n");
                    } else if ("assistant".equals(role)) {
                        historyText.append("Assistant: ").append(content).append("\n\n");
                    } else if ("system".equals(role)) {
                        // 系统消息通常不显示在历史中，但如果需要可以取消注释
                        // historyText.append("System: ").append(content).append("\n\n");
                    }
                }

                builder.addSection("Conversation History", historyText.toString());
            }
        }

        // 添加当前用户提示
        builder.addSection("Current Request", prompt);

        // 如果需要Markdown格式输出
        if (isMarkdown) {
            builder.addRawText("Please format your response using Markdown where appropriate for better readability.");
        }

        // 获取额外的上下文信息
        String contextInfo = getStringValue(params, "contextInfo", null);
        if (contextInfo != null && !contextInfo.isEmpty()) {
            builder.addSection("Additional Context", contextInfo);
        }

        return builder.build();
    }

    @Override
    public String parseResponse(String aiResponse) {
        // 对于文本生成，我们直接返回AI的响应
        // 如果未来需要特定格式的解析，可以使用AIResponseParser的方法
        return aiResponse;
    }

    @Override
    public void validate(Map<String, Object> params) {
        if (params == null) {
            throw new OperationException("text-generation", "Parameters cannot be null");
        }

        if (!params.containsKey("prompt") || params.get("prompt") == null) {
            throw new OperationException("text-generation", "Missing required parameter: prompt");
        }

        // 验证历史记录格式
        if (params.containsKey("history")) {
            Object history = params.get("history");
            if (!(history instanceof List)) {
                throw new OperationException("text-generation", "History must be a list of messages");
            }

            try {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> historyList = (List<Map<String, String>>) history;

                for (Map<String, String> message : historyList) {
                    if (!message.containsKey("role") || !message.containsKey("content")) {
                        throw new OperationException("text-generation",
                                "Each history message must contain 'role' and 'content' fields");
                    }

                    String role = message.get("role");
                    if (!("user".equals(role) || "assistant".equals(role) || "system".equals(role))) {
                        throw new OperationException("text-generation",
                                "Message role must be 'user', 'assistant', or 'system'");
                    }
                }
            } catch (ClassCastException e) {
                throw new OperationException("text-generation",
                        "Invalid history format. Expected List<Map<String, String>>");
            }
        }
    }
}