package com.suifeng.sfchain.persistence.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息类型：SYSTEM, USER, ASSISTANT
     */
    public enum MessageType {
        SYSTEM,     // 系统提示词
        USER,       // 用户消息
        ASSISTANT   // AI回复
    }

    private String id;
    private MessageType type;
    private String content;
    private LocalDateTime timestamp;
    private String sessionId;

    public static ChatMessage systemMessage(String sessionId, String content) {
        return new ChatMessage(
                generateId(),
                MessageType.SYSTEM,
                content,
                LocalDateTime.now(),
                sessionId
        );
    }

    public static ChatMessage userMessage(String sessionId, String content) {
        return new ChatMessage(
                generateId(),
                MessageType.USER,
                content,
                LocalDateTime.now(),
                sessionId
        );
    }

    public static ChatMessage assistantMessage(String sessionId, String content) {
        return new ChatMessage(
                generateId(),
                MessageType.ASSISTANT,
                content,
                LocalDateTime.now(),
                sessionId
        );
    }

    private static String generateId() {
        return System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}