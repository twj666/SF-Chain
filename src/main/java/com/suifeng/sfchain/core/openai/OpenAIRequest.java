package com.suifeng.sfchain.core.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 描述: OpenAI兼容的请求体
 * @author suifeng
 * 日期: 2025/8/11
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIRequest {
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 消息列表
     */
    private List<Message> messages;
    
    /**
     * 最大token数
     */
    private Integer max_tokens;
    
    /**
     * 温度参数 (0.0-2.0)
     */
    private Double temperature;
    
    /**
     * 是否流式输出
     */
    private Boolean stream;
    
    /**
     * 响应格式
     */
    private Map<String, Object> response_format;
    
    /**
     * 是否启用思考模式 (部分模型支持)
     */
    private Boolean enable_thinking;
    
    /**
     * top_p参数
     */
    private Double top_p;
    
    /**
     * 频率惩罚
     */
    private Double frequency_penalty;
    
    /**
     * 存在惩罚
     */
    private Double presence_penalty;
    
    /**
     * 停止词
     */
    private List<String> stop;
    
    /**
     * 用户标识
     */
    private String user;
    
    @Data
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        /**
         * 角色: system, user, assistant
         */
        private String role;
        
        /**
         * 消息内容
         */
        private String content;
        
        /**
         * 消息名称 (可选)
         */
        private String name;
    }
}