package com.sfchain.operations.common;

import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.exception.OperationException;
import com.sfchain.core.operation.AIOperation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 描述: 简单文本生成操作
 * @author suifeng
 * 日期: 2025/4/15
 */
@AIOp("text-generation")
@Component
public class TextGenerationOperation implements AIOperation<Map<String, Object>, String> {
    
    @Override
    public List<String> supportedModels() {
        return List.of("deepseek-chat", "qwen-plus", "gpt-4o");
    }
    
    @Override
    public String buildPrompt(Map<String, Object> params) {
        if (!params.containsKey("prompt")) {
            throw new OperationException("text-generation", "Missing required parameter: prompt");
        }
        
        String prompt = params.get("prompt").toString();
        String systemPrompt = params.containsKey("systemPrompt") ? 
                params.get("systemPrompt").toString() : null;
        
        StringBuilder fullPrompt = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            fullPrompt.append("System: ").append(systemPrompt).append("\n\n");
        }
        
        fullPrompt.append(prompt);
        
        return fullPrompt.toString();
    }
    
    @Override
    public String parseResponse(String aiResponse) {
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
    }
}