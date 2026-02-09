package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 远程日志治理快照
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteIngestionGovernanceSnapshot {

    /**
     * 接入契约版本白名单
     */
    private List<String> contractAllowlist = new ArrayList<>();

    /**
     * 是否请求立即重建索引
     */
    private boolean rebuildIndexes;
}
