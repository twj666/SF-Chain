package com.sfchain.models.siliconflow;

import org.springframework.stereotype.Component;

/**
 * 描述: DeepSeek R1模型实现
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class DeepSeekR1Model extends AbstractSiliconFlowModel {
    
    public static final String MODEL_NAME = "deepseek-r1";
    
    /**
     * 构造函数
     * 
     * @param config 模型配置
     */
    public DeepSeekR1Model(SiliconflowConfig config) {
        super(config);
    }
    
    @Override
    public String getName() {
        return MODEL_NAME;
    }
    
    @Override
    public String description() {
        return "深度求索 (DeepSeek R1) 深度思考";
    }
    
    @Override
    protected String getModelVersion() {
        return "Pro/deepseek-ai/DeepSeek-R1";
    }
}