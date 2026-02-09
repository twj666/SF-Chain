package com.suifeng.sfchain.config.remote;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志治理同步应用结果
 */
@Data
public class GovernanceSyncApplyResult {

    private boolean valid;

    private boolean applied;

    private boolean targeted;

    private boolean rolledBack;

    private int rebuilt;

    private long sampleCount;

    private double rejectRate;

    private String releaseId;

    private String stage;

    private GovernanceReleaseStatus status;

    private String reasonCode;

    private long nextRetryAtEpochMs;

    private long eventTimeEpochMs;

    private String message;

    private List<String> requestedVersions = new ArrayList<>();

    private List<String> activeVersions = new ArrayList<>();
}
