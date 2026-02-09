package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.openai.OpenAIModelConfig;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 远程配置快照
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConfigSnapshot {

    private String version;

    private boolean notModified;

    private Map<String, OpenAIModelConfig> models = new HashMap<>();

    private Map<String, String> operationModelMapping = new HashMap<>();

    private Map<String, AIOperationRegistry.OperationConfig> operationConfigs = new HashMap<>();

    private RemoteIngestionGovernanceSnapshot ingestionGovernance;
}
