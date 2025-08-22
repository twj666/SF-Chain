package com.suifeng.sfchain.core.openai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 描述: OpenAI兼容的响应体
 * @author suifeng
 * 日期: 2025/8/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIResponse {
    
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
    private List<Choice> choices;
    
    /**
     * 使用情况
     */
    private Usage usage;
    
    /**
     * 系统指纹
     */
    private String system_fingerprint;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Choice {
        /**
         * 选择索引
         */
        private Integer index;
        
        /**
         * 消息内容
         */
        private OpenAIRequest.Message message;
        
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
    public static class Usage {
        /**
         * 提示token数
         */
        private Integer prompt_tokens;
        
        /**
         * 完成token数
         */
        private Integer completion_tokens;
        
        /**
         * 总token数
         */
        private Integer total_tokens;
        
        /**
         * 提示token详情 (可选)
         */
        private Object prompt_tokens_details;
        
        /**
         * 完成token详情 (可选)
         */
        private Object completion_tokens_details;
    }
}