package com.sfchain.models.flow.model;

import com.sfchain.models.flow.AbstractSiliconFlowModel;
import com.sfchain.models.flow.SiliconflowConfig;
import org.springframework.stereotype.Component;

import static com.sfchain.core.constant.AIConstant.SILI_QWEN;

/**
 * 描述: 千问模型实现
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class QWenModel extends AbstractSiliconFlowModel {

    /**
     * 构造函数
     *
     * @param config 模型配置
     */
    public QWenModel(SiliconflowConfig config) {
        super(config);
    }
    
    @Override
    public String getName() {
        return SILI_QWEN;
    }
    
    @Override
    public String description() {
        return "通义千问Qwen2.5-32B";
    }
    
    @Override
    protected String getModelVersion() {
        return SILI_QWEN;
    }
}