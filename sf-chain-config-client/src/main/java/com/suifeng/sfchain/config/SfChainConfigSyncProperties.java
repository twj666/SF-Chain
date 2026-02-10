package com.suifeng.sfchain.config;

import com.suifeng.sfchain.config.remote.GovernanceInvalidCursorStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SF-Chain 配置同步参数
 */
@Data
@ConfigurationProperties(prefix = "sf-chain.config-sync")
public class SfChainConfigSyncProperties {

    /**
     * 是否启用远程配置同步
     */
    private boolean enabled = false;

    /**
     * 同步间隔（秒）
     */
    private int intervalSeconds = 30;

    /**
     * 远程失败时是否容错继续运行
     */
    private boolean failOpen = true;

    /**
     * 启动时是否要求远程同步成功（支持重试）
     */
    private boolean startupCheckEnabled = true;

    /**
     * 启动阶段同步最大重试次数
     */
    private int startupMaxAttempts = 3;

    /**
     * 启动阶段同步重试间隔（毫秒）
     */
    private long startupRetryIntervalMs = 2000;

    /**
     * 本地快照缓存文件
     */
    private String cacheFile = ".sf-chain/config-snapshot.json";

    /**
     * 是否启用日志治理配置同步
     */
    private boolean ingestionGovernanceEnabled = true;

    /**
     * 是否上报日志治理配置应用结果
     */
    private boolean governanceFeedbackEnabled = true;

    /**
     * 是否上报治理状态事件
     */
    private boolean governanceEventEnabled = true;

    /**
     * 是否上报治理终态回调
     */
    private boolean governanceFinalizeEnabled = true;

    /**
     * 治理同步运行时状态文件
     */
    private String governanceStateFile = ".sf-chain/governance-sync-state.json";

    /**
     * 是否启用治理租约锁（多实例仲裁）
     */
    private boolean governanceLeaseEnabled = true;

    /**
     * 治理租约锁文件路径
     */
    private String governanceLeaseFile = ".sf-chain/governance-sync.lock";

    /**
     * finalize ACK 重试间隔（秒）
     */
    private int governanceFinalizeRetrySeconds = 30;

    /**
     * finalized 状态保留上限
     */
    private int governanceStateMaxFinalized = 2000;

    /**
     * pending finalize 保留上限
     */
    private int governanceStateMaxPending = 1000;

    /**
     * 是否启用远程租约锁
     */
    private boolean governanceRemoteLeaseEnabled = false;

    /**
     * 远程租约ttl（秒）
     */
    private int governanceRemoteLeaseTtlSeconds = 30;

    /**
     * 是否启用finalize对账拉取
     */
    private boolean governanceFinalizeReconcileEnabled = false;

    /**
     * finalize对账单次最大分页拉取次数
     */
    private int governanceFinalizeReconcileMaxPages = 5;

    /**
     * finalize对账无效游标处理策略
     */
    private GovernanceInvalidCursorStrategy governanceFinalizeReconcileInvalidCursorStrategy =
            GovernanceInvalidCursorStrategy.RESET_AND_RETRY;

    /**
     * 无效游标告警阈值（达到阈值及其倍数时告警）
     */
    private int governanceFinalizeReconcileInvalidCursorWarnThreshold = 10;

    /**
     * 鏄惁鍦ㄥ惎鍔ㄦ椂涓婃姤鏈湴鎿嶄綔鑺傜偣鐩綍鍒伴厤缃腑蹇?
     */
    private boolean operationCatalogSyncEnabled = true;
}
