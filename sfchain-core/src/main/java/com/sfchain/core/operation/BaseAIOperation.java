package com.sfchain.core.operation;


import java.util.Map;

/**
 * 基础AI操作类
 * 提供通用的AI操作实现，简化子类开发
 * @author suifeng
 * 日期: 2025/04/18
 */
public abstract class BaseAIOperation<P, R> implements AIOperation<P, R> {
    
    /**
     * 创建提示词构建器
     * @param title 提示词标题
     * @return 提示词构建器
     */
    protected AIPromptBuilder createPromptBuilder(String title) {
        return new AIPromptBuilder(title);
    }
    
    /**
     * 从AI响应中提取JSON
     * @param aiResponse AI响应
     * @return JSON字符串
     */
    protected String extractJsonFromResponse(String aiResponse) {
        return AIResponseParser.extractJson(aiResponse);
    }
    
    /**
     * 从AI响应中提取表格
     * @param aiResponse AI响应
     * @return 表格数据
     */
    protected String[] extractTableFromResponse(String aiResponse) {
        return AIResponseParser.extractTable(aiResponse);
    }
    
    /**
     * 从AI响应中提取指定类型的代码块
     * @param aiResponse AI响应
     * @param codeType 代码类型
     * @return 代码块内容
     */
    protected String extractCodeBlockFromResponse(String aiResponse, String codeType) {
        return AIResponseParser.extractCodeBlock(aiResponse, codeType);
    }
    
    /**
     * 从Map中安全获取字符串值
     * @param map Map对象
     * @param key 键
     * @param defaultValue 默认值
     * @return 值
     */
    protected String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 从Map中安全获取整数值
     * @param map Map对象
     * @param key 键
     * @param defaultValue 默认值
     * @return 值
     */
    protected Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * 获取Map中的双精度值
     */
    protected Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        if (map == null) {
            return defaultValue;
        }

        if (map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                try {
                    return Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * 从Map中安全获取布尔值
     * @param map Map对象
     * @param key 键
     * @param defaultValue 默认值
     * @return 值
     */
    protected Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * 从Map中安全获取列表值
     * @param map Map对象
     * @param key 键
     * @return 值
     */
    protected <T> java.util.List<T> getListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof java.util.List) {
            return (java.util.List<T>) value;
        }
        return new java.util.ArrayList<>();
    }
}