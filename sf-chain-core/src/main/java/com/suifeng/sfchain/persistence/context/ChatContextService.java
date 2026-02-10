package com.suifeng.sfchain.persistence.context;

import java.util.List;

/**
 * 描述:
 * @author suifeng
 * 日期: 2025/8/14
 */
public interface ChatContextService {

    /**
     * 设置或更新系统提示词
     * @param sessionId 会话ID
     * @param systemPrompt 系统提示词内容
     */
    void setSystemPrompt(String sessionId, String systemPrompt);

    /**
     * 获取系统提示词
     * @param sessionId 会话ID
     * @return 系统提示词内容，如果不存在返回null
     */
    String getSystemPrompt(String sessionId);

    /**
     * 添加用户消息到会话上下文
     * @param sessionId 会话ID
     * @param userMessage 用户消息内容
     */
    void addUserMessage(String sessionId, String userMessage);

    /**
     * 添加AI回复到会话上下文
     * @param sessionId 会话ID
     * @param aiResponse AI回复内容
     */
    void  addAiResponse(String sessionId, String aiResponse);

    /**
     * 获取完整的对话上下文（包含系统提示词）
     * @param sessionId 会话ID
     * @return 完整的消息列表，系统提示词在首位
     */
    List<ChatMessage> getFullContext(String sessionId);

    /**
     * 获取对话历史（不包含系统提示词）
     * @param sessionId 会话ID
     * @return 用户和AI的对话历史
     */
    List<ChatMessage> getConversationHistory(String sessionId);

    /**
     * 获取格式化的上下文字符串
     * @param sessionId 会话ID
     * @param includeSystemPrompt 是否包含系统提示词
     * @return 格式化的上下文字符串
     */
    String getContextAsString(String sessionId, boolean includeSystemPrompt);

    /**
     * 清除会话的对话历史（保留系统提示词）
     * @param sessionId 会话ID
     */
    void clearConversation(String sessionId);

    /**
     * 完全清除会话（包括系统提示词）
     * @param sessionId 会话ID
     */
    void clearSession(String sessionId);

    /**
     * 检查会话是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean sessionExists(String sessionId);

    /**
     * 获取对话消息数量（不包含系统提示词）
     * @param sessionId 会话ID
     * @return 消息数量
     */
    int getConversationMessageCount(String sessionId);
}