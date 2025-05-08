package com.sfchain.models.flow.model;

import com.sfchain.models.flow.AbstractSiliconFlowModel;
import com.sfchain.models.flow.SiliconflowConfig;
import org.springframework.stereotype.Component;

import static com.sfchain.core.constant.AIConstant.SILI_DEEP_SEEK_V3;

/**
 * 描述: DeepSeek R1模型实现
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class DeepSeekV3Model extends AbstractSiliconFlowModel {

    /**
     * 构造函数
     *
     * @param config 模型配置
     */
    public DeepSeekV3Model(SiliconflowConfig config) {
        super(config);
    }
    
    @Override
    public String getName() {
        return SILI_DEEP_SEEK_V3;
    }
    
    @Override
    public String description() {
        return "深度求索 (DeepSeek R1) 深度思考";
    }
    
    @Override
    protected String getModelVersion() {
        return SILI_DEEP_SEEK_V3;
    }
}