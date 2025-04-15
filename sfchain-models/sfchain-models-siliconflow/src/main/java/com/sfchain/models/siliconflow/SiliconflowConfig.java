package com.sfchain.models.siliconflow;


import com.sfchain.core.model.ModelConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 描述: 鬼节流动模型配置信息
 * @author suifeng
 * 日期: 2025/4/15
 */
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "sfchain.models.siliconflow")
@Component
@Data
public class SiliconflowConfig extends ModelConfig {

    private String version = "deepseek-ai/DeepSeek-V3";

    private Double temperature = 0.7;

    private Integer maxTokens = 2048;
}