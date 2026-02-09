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

    private int rebuilt;

    private String message;

    private List<String> requestedVersions = new ArrayList<>();

    private List<String> activeVersions = new ArrayList<>();
}
