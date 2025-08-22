package com.suifeng.sfchain.core;

/**
 * 描述: AI模型接口
 * @author suifeng
 * 日期: 2025/8/11
 */
public interface AIModel {
    
    /**
     * 获取模型名称
     */
    String getName();
    
    /**
     * 获取模型描述
     */
    String description();
    
    /**
     * 生成文本响应
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(String prompt);
    
    /**
     * 生成指定类型的响应
     * @param prompt 提示词
     * @param responseType 响应类型
     * @return 生成的响应对象
     */
    <T> T generate(String prompt, Class<T> responseType);
    
    /**
     * 检查模型是否可用
     * @return 是否可用
     */
    boolean isAvailable();
}