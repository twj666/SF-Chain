package com.sfchain.models.flow.model;

import com.sfchain.models.flow.AbstractSiliconFlowModel;
import com.sfchain.models.flow.SiliconflowConfig;
import org.springframework.stereotype.Component;

import static com.sfchain.core.constant.AIConstant.SILI_THUDM;

/**
 * 描述: 智谱清言模型实现
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class THUDMModel extends AbstractSiliconFlowModel {

    /**
     * 构造函数
     *
     * @param config 模型配置
     */
    public THUDMModel(SiliconflowConfig config) {
        super(config);
    }
    
    @Override
    public String getName() {
        return SILI_THUDM;
    }
    
    @Override
    public String description() {
        return "智谱清言glm-4-9b-chat";
    }
    
    @Override
    protected String getModelVersion() {
        return SILI_THUDM;
    }
}