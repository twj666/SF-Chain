package com.suifeng.sfchain.persistence.context;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: 
 * @author suifeng
 * 日期: 2025/8/14 
 */
@Slf4j
@Service
public class MapBasedChatContextService implements ChatContextService {

    // 系统提示词存储
    private final Map<String, String> systemPrompts = new ConcurrentHashMap<>();

    // 对话历史存储
    private final Map<String, List<ChatMessage>> conversationHistories = new ConcurrentHashMap<>();

    private static final int MAX_MESSAGES_PER_SESSION = 20;

    @Override
    public void setSystemPrompt(String sessionId, String systemPrompt) {
        if (sessionId == null || systemPrompt == null) {
            log.warn("会话ID或系统提示词为空，跳过设置");
            return;
        }

        systemPrompts.put(sessionId, systemPrompt);
        log.debug("设置系统提示词: sessionId={}", sessionId);
    }

    @Override
    public String getSystemPrompt(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return systemPrompts.get(sessionId);
    }

    @Override
    public void addUserMessage(String sessionId, String userMessage) {
        if (sessionId == null || userMessage == null) {
            log.warn("会话ID或用户消息为空，跳过添加");
            return;
        }

        ChatMessage message = ChatMessage.userMessage(sessionId, userMessage);
        addConversationMessage(sessionId, message);
        log.debug("添加用户消息: sessionId={}", sessionId);
    }

    @Override
    public void addAiResponse(String sessionId, String aiResponse) {
        if (sessionId == null || aiResponse == null) {
            log.warn("会话ID或AI回复为空，跳过添加");
            return;
        }

        ChatMessage message = ChatMessage.assistantMessage(sessionId, aiResponse);
        addConversationMessage(sessionId, message);
        log.debug("添加AI回复: sessionId={}", sessionId);
    }

    private void addConversationMessage(String sessionId, ChatMessage message) {
        conversationHistories.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);

        List<ChatMessage> messages = conversationHistories.get(sessionId);
        while (messages.size() > MAX_MESSAGES_PER_SESSION) {
            messages.remove(0);
            log.debug("对话历史超限，移除最旧消息: sessionId={}", sessionId);
        }
    }

    @Override
    public List<ChatMessage> getFullContext(String sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> fullContext = new ArrayList<>();

        // 添加系统提示词
        String systemPrompt = getSystemPrompt(sessionId);
        if (systemPrompt != null) {
            fullContext.add(ChatMessage.systemMessage(sessionId, systemPrompt));
        }

        // 添加对话历史
        fullContext.addAll(getConversationHistory(sessionId));

        return fullContext;
    }

    @Override
    public List<ChatMessage> getConversationHistory(String sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(conversationHistories.getOrDefault(sessionId, new ArrayList<>()));
    }

    @Override
    public String getContextAsString(String sessionId, boolean includeSystemPrompt) {
        List<ChatMessage> messages;

        if (includeSystemPrompt) {
            messages = getFullContext(sessionId);
        } else {
            messages = getConversationHistory(sessionId);
        }

        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (ChatMessage message : messages) {
            String role = getRoleString(message.getType());
            context.append(role).append(": ").append(message.getContent()).append("\n");
        }

        return context.toString();
    }

    @Override
    public void clearConversation(String sessionId) {
        if (sessionId != null) {
            conversationHistories.remove(sessionId);
            log.info("清除对话历史: sessionId={}", sessionId);
        }
    }

    @Override
    public void clearSession(String sessionId) {
        if (sessionId != null) {
            systemPrompts.remove(sessionId);
            conversationHistories.remove(sessionId);
            log.info("完全清除会话: sessionId={}", sessionId);
        }
    }

    @Override
    public boolean sessionExists(String sessionId) {
        return sessionId != null &&
                (systemPrompts.containsKey(sessionId) || conversationHistories.containsKey(sessionId));
    }

    @Override
    public int getConversationMessageCount(String sessionId) {
        if (sessionId == null) {
            return 0;
        }
        return conversationHistories.getOrDefault(sessionId, new ArrayList<>()).size();
    }

    private String getRoleString(ChatMessage.MessageType type) {
        switch (type) {
            case SYSTEM:
                return "系统";
            case USER:
                return "用户";
            case ASSISTANT:
                return "助手";
            default:
                return "未知";
        }
    }
}