package com.sfchain.core.operation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI响应解析器
 * 用于从AI响应中提取特定格式的内容
 * @author suifeng
 * 日期: 2025/04/18
 */
public class AIResponseParser {
    
    // 代码块正则表达式模式
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:\\w+)?\\s*(.+?)\\s*```", Pattern.DOTALL);
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`(.+?)`", Pattern.DOTALL);
    
    // 表格正则表达式模式
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\|(.+?)\\|\\s*(?:\\n|$)", Pattern.DOTALL);
    
    /**
     * 从AI响应中提取JSON
     * @param response AI响应文本
     * @return 提取的JSON字符串
     */
    public static String extractJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("AI响应为空");
        }

        try {
            // 1. 尝试查找完整的JSON对象（从第一个{到最后一个}）
            int startIndex = response.indexOf('{');
            int endIndex = response.lastIndexOf('}');

            if (startIndex >= 0 && endIndex > startIndex) {
                return response.substring(startIndex, endIndex + 1).trim();
            }

            // 2. 尝试查找JSON代码块
            Matcher jsonBlockMatcher = JSON_BLOCK_PATTERN.matcher(response);
            if (jsonBlockMatcher.find()) {
                String extracted = jsonBlockMatcher.group(1).trim();
                if (isValidJson(extracted)) {
                    return extracted;
                }
            }

            // 3. 尝试查找任意代码块
            Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(response);
            if (codeBlockMatcher.find()) {
                String extracted = codeBlockMatcher.group(1).trim();
                if (isValidJson(extracted)) {
                    return extracted;
                }
            }

            // 4. 尝试查找内联代码
            Matcher inlineCodeMatcher = INLINE_CODE_PATTERN.matcher(response);
            if (inlineCodeMatcher.find()) {
                String extracted = inlineCodeMatcher.group(1).trim();
                if (isValidJson(extracted)) {
                    return extracted;
                }
            }

            // 5. 如果以上方法都失败，尝试直接解析整个响应
            if (isValidJson(response.trim())) {
                return response.trim();
            }

            // 如果无法提取有效的JSON，抛出异常
            throw new IllegalArgumentException("无法从AI响应中提取有效的JSON");
        } catch (Exception e) {
            throw new RuntimeException("提取JSON失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从AI响应中提取表格数据
     * @param response AI响应文本
     * @return 表格数据的字符串数组，每行一个元素
     */
    public static String[] extractTable(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("AI响应为空");
        }
        
        try {
            Matcher tableMatcher = TABLE_PATTERN.matcher(response);
            StringBuilder tableBuilder = new StringBuilder();
            
            while (tableMatcher.find()) {
                tableBuilder.append(tableMatcher.group(1).trim()).append("\n");
            }
            
            if (tableBuilder.length() > 0) {
                return tableBuilder.toString().split("\n");
            }
            
            throw new IllegalArgumentException("无法从AI响应中提取表格数据");
        } catch (Exception e) {
            throw new RuntimeException("提取表格数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从AI响应中提取指定类型的代码块
     * @param response AI响应文本
     * @param codeType 代码类型（如"json", "xml", "yaml"等）
     * @return 提取的代码块内容
     */
    public static String extractCodeBlock(String response, String codeType) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("AI响应为空");
        }
        
        try {
            Pattern pattern = Pattern.compile("```(?:" + codeType + ")?\\s*(.+?)\\s*```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            
            throw new IllegalArgumentException("无法从AI响应中提取" + codeType + "代码块");
        } catch (Exception e) {
            throw new RuntimeException("提取代码块失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查字符串是否为有效的JSON
     * @param jsonStr 要检查的JSON字符串
     * @return 是否有效
     */
    private static boolean isValidJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        
        // 简单检查JSON格式
        jsonStr = jsonStr.trim();
        return (jsonStr.startsWith("{") && jsonStr.endsWith("}")) || 
               (jsonStr.startsWith("[") && jsonStr.endsWith("]"));
    }
}