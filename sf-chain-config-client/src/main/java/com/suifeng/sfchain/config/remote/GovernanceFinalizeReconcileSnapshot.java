package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * finalize 对账快照
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GovernanceFinalizeReconcileSnapshot {
    private List<String> ackedTaskKeys = new ArrayList<>();
    private String nextCursor;
    private boolean hasMore;
}
