package com.suifeng.sfchain.core.openai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 描述: OpenAI兼容的流式响应体
 * @author suifeng
 * 日期: 2025/8/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIStreamResponse {
    
    /**
     * 响应ID
     */
    private String id;
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 创建时间戳
     */
    private Long created;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 选择列表
     */
    private List<StreamChoice> choices;
    
    /**
     * 系统指纹
     */
    private String system_fingerprint;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StreamChoice {
        /**
         * 选择索引
         */
        private Integer index;
        
        /**
         * 增量消息内容
         */
        private Delta delta;
        
        /**
         * 完成原因
         */
        private String finish_reason;
        
        /**
         * logprobs (可选)
         */
        private Object logprobs;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Delta {
        /**
         * 角色 (仅在第一个chunk中出现)
         */
        private String role;
        
        /**
         * 增量内容
         */
        private String content;
    }
}