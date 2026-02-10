package com.suifeng.sfchain.core;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * AI提示词构建器
 * 用于构建结构化的AI提示词，支持与AIResponseParser配合使用
 * @author suifeng
 * 日期: 2025/04/18
 */
public class AIPromptBuilder {

    private final StringBuilder promptBuilder = new StringBuilder();
    private boolean hasJsonOutput = false;
    
    /**
     * 创建一个提示词构建器
     * @param title 提示词标题
     */
    public AIPromptBuilder(String title) {
        promptBuilder.append("# ").append(title).append("\n\n");
    }
    
    /**
     * 添加角色描述
     * @param roleDescription 角色描述
     * @return 构建器实例
     */
    public AIPromptBuilder addRole(String roleDescription) {
        // 获取当前北京时间
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
        promptBuilder.append("# 当前的时间是北京时间：").append(formattedTime).append("\n");
        promptBuilder.append(roleDescription).append("\n\n");
        return this;
    }
    
    /**
     * 添加章节
     * @param sectionTitle 章节标题
     * @param content 章节内容
     * @return 构建器实例
     */
    public AIPromptBuilder addSection(String sectionTitle, String content) {
        promptBuilder.append("## ").append(sectionTitle).append("\n");
        promptBuilder.append(content).append("\n\n");
        return this;
    }
    
    /**
     * 添加子章节
     * @param subSectionTitle 子章节标题
     * @param content 子章节内容
     * @return 构建器实例
     */
    public AIPromptBuilder addSubSection(String subSectionTitle, String content) {
        promptBuilder.append("### ").append(subSectionTitle).append("\n");
        promptBuilder.append(content).append("\n\n");
        return this;
    }
    
    /**
     * 添加JSON输出格式
     * @param jsonExample JSON示例
     * @return 构建器实例
     */
    public AIPromptBuilder addJsonOutput(String jsonExample) {
        promptBuilder.append("## 输出格式要求\n");
        promptBuilder.append("所有字符串中的引用部分只能用英文单引号 ' 包裹，内容中禁止出现英文双引号, 但是你要注意json格式规范是要加\"的，保证我能正常解析JSON 。\n");
        promptBuilder.append("```json\n");
        promptBuilder.append(jsonExample).append("\n");
        promptBuilder.append("```\n");
        promptBuilder.append("不需要给我任何额外的信息，我只需要最终的json结果，以保证生成和响应的速度\n");
        hasJsonOutput = true;
        return this;
    }
    
    /**
     * 添加表格输出格式
     * @param headers 表头
     * @param example 示例行
     * @return 构建器实例
     */
    public AIPromptBuilder addTableOutput(String[] headers, String[] example) {
        promptBuilder.append("## 输出格式要求\n");
        promptBuilder.append("你必须严格按照以下表格格式返回结果：\n");
        promptBuilder.append("```\n");
        
        // 构建表头
        promptBuilder.append("| ");
        for (String header : headers) {
            promptBuilder.append(header).append(" | ");
        }
        promptBuilder.append("\n");
        
        // 构建分隔行
        promptBuilder.append("| ");
        for (int i = 0; i < headers.length; i++) {
            promptBuilder.append("--- | ");
        }
        promptBuilder.append("\n");
        
        // 构建示例行
        if (example != null) {
            promptBuilder.append("| ");
            for (String cell : example) {
                promptBuilder.append(cell).append(" | ");
            }
            promptBuilder.append("\n");
        }
        
        promptBuilder.append("```\n\n");
        return this;
    }
    
    /**
     * 添加自定义输出格式
     * @param formatDescription 格式描述
     * @param example 示例
     * @param formatType 格式类型（用于AIResponseParser识别）
     * @return 构建器实例
     */
    public AIPromptBuilder addCustomOutput(String formatDescription, String example, String formatType) {
        promptBuilder.append("## 输出格式要求\n");
        promptBuilder.append(formatDescription).append("\n");
        promptBuilder.append("```").append(formatType).append("\n");
        promptBuilder.append(example).append("\n");
        promptBuilder.append("```\n\n");
        return this;
    }
    
    /**
     * 添加原始文本（不添加任何格式）
     * @param text 原始文本
     * @return 构建器实例
     */
    public AIPromptBuilder addRawText(String text) {
        promptBuilder.append(text).append("\n\n");
        return this;
    }
    
    /**
     * 构建提示词
     * @return 完整的提示词字符串
     */
    public String build() {
        if (!hasJsonOutput) {
            // 如果没有指定输出格式，添加默认提示
            promptBuilder.append("请确保你的回答简洁明了，直接提供所需信息。\n");
        }
        return promptBuilder.toString();
    }
}