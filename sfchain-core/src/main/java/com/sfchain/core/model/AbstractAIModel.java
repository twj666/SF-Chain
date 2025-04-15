package com.sfchain.core.model;

import com.sfchain.core.exception.ModelException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述: 抽象AI模型实现，提供通用功能
 * @author suifeng
 * 日期: 2025/4/15
 */
public abstract class AbstractAIModel implements AIModel {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractAIModel.class);
    
    /**
     * 模型配置
     */
    protected final ModelConfig config;
    
    /**
     * 模型参数
     */
    @Getter
    protected ModelParameters parameters;
    
    /**
     * 构造函数
     * 
     * @param config 模型配置
     */
    public AbstractAIModel(ModelConfig config) {
        this.config = config;
        this.parameters = new ModelParameters();
    }
    
    @Override
    public String generate(String prompt) {
        logger.debug("Generating content with model: {}", getName());
        long startTime = System.currentTimeMillis();
        
        try {
            String result = doGenerate(prompt);
            
            long endTime = System.currentTimeMillis();
            logger.debug("Generation completed in {}ms", (endTime - startTime));
            
            return result;
        } catch (Exception e) {
            logger.error("Error generating content with model: {}", getName(), e);
            throw new ModelException(getName(), e.getMessage(), e);
        }
    }
    
    @Override
    public AIModel withParameters(ModelParameters parameters) {
        this.parameters = new ModelParameters(parameters);
        return this;
    }
    
    /**
     * 实际生成内容的方法，由子类实现
     * 
     * @param prompt 提示词
     * @return 生成的内容
     * @throws Exception 如果生成过程中发生错误
     */
    protected abstract String doGenerate(String prompt) throws Exception;
}