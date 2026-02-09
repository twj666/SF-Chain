package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志治理灰度发布计划
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteGovernanceRolloutPlan {

    private String releaseId;

    /**
     * CANARY 或 FULL
     */
    private String stage = "FULL";

    /**
     * CANARY 阶段生效流量百分比 [0,100]
     */
    private int trafficPercent = 100;

    /**
     * 可接受的契约拒绝率阈值
     */
    private double maxRejectRate = 0.2D;

    /**
     * 触发阈值判断的最小样本数
     */
    private long minSamples = 20;

    /**
     * 违反阈值时是否自动回滚
     */
    private boolean rollbackOnViolation = true;

    /**
     * 回滚目标白名单
     */
    private List<String> rollbackAllowlist = new ArrayList<>();
}
