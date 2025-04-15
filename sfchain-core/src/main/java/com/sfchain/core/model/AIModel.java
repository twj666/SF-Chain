package com.sfchain.core.model;

/**
 * 描述: AI模型接口，定义所有AI模型的基本行为
 * @author suifeng
 * 日期: 2025/4/15
 */
public interface AIModel {
    
    /**
     * 获取模型名称
     * 
     * @return 模型唯一标识名称
     */
    String getName();
    
    /**
     * 获取模型描述
     * 
     * @return 模型描述信息
     */
    String description();
    
    /**
     * 生成文本内容
     * 
     * @param prompt 提示词
     * @return 生成的文本内容
     */
    String generate(String prompt);
    
    /**
     * 生成并解析为指定类型
     * 
     * @param prompt 提示词
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 解析后的对象
     */
    <T> T generate(String prompt, Class<T> responseType);
    
    /**
     * 获取模型参数
     * 
     * @return 模型参数对象
     */
    default ModelParameters getParameters() {
        return new ModelParameters();
    }
    
    /**
     * 设置模型参数
     * 
     * @param parameters 模型参数
     * @return 模型实例(链式调用)
     */
    default AIModel withParameters(ModelParameters parameters) {
        return this;
    }
}