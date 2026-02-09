package com.suifeng.sfchain.config.remote;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 治理同步运行时状态快照
 */
@Data
public class GovernanceSyncRuntimeState {

    private IngestionGovernanceSyncApplier.ApplierState applierState;

    private Map<String, GovernanceFinalizeRecord> finalizedStates = new LinkedHashMap<>();

    private Map<String, GovernanceFinalizeTask> pendingFinalizations = new LinkedHashMap<>();
}
