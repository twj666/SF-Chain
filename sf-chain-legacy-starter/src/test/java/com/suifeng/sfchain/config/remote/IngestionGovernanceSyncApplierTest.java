package com.suifeng.sfchain.config.remote;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import com.suifeng.sfchain.core.logging.ingestion.IngestionContractHealthTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionGovernanceSyncApplierTest {

    @Test
    void shouldApplyAllowlistWhenGuardValidationPasses() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );

        RemoteIngestionGovernanceSnapshot snapshot = new RemoteIngestionGovernanceSnapshot();
        snapshot.setContractAllowlist(List.of("v2", "v1"));

        GovernanceSyncApplyResult result = applier.apply(snapshot);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isApplied()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovernanceReleaseStatus.SUCCEEDED);
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v2");
        assertThat(properties.getSupportedContractVersions()).containsExactly("v2", "v1");
    }

    @Test
    void shouldRejectAllowlistWhenNoOverlapRequired() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        properties.setRequireCurrentVersionOverlap(true);
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );

        RemoteIngestionGovernanceSnapshot snapshot = new RemoteIngestionGovernanceSnapshot();
        snapshot.setContractAllowlist(List.of("v3"));

        GovernanceSyncApplyResult result = applier.apply(snapshot);

        assertThat(result.isValid()).isFalse();
        assertThat(result.isApplied()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovernanceReleaseStatus.FAILED);
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v1");
    }

    @Test
    void shouldSkipCanaryWhenAppNotInCohort() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "app-outside-cohort"
        );

        RemoteIngestionGovernanceSnapshot snapshot = new RemoteIngestionGovernanceSnapshot();
        snapshot.setContractAllowlist(List.of("v2", "v1"));
        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setStage("CANARY");
        rollout.setTrafficPercent(0);
        snapshot.setRollout(rollout);

        GovernanceSyncApplyResult result = applier.apply(snapshot);

        assertThat(result.isTargeted()).isFalse();
        assertThat(result.isApplied()).isFalse();
        assertThat(result.getStatus()).isEqualTo(GovernanceReleaseStatus.SKIPPED);
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v1");
    }

    @Test
    void shouldRollbackCanaryWhenRejectRateTooHigh() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionContractHealthTracker healthTracker = new IngestionContractHealthTracker();
        healthTracker.recordContractRejected();
        healthTracker.recordContractRejected();
        healthTracker.recordAccepted();
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                healthTracker,
                "app-in-canary"
        );

        RemoteIngestionGovernanceSnapshot snapshot = new RemoteIngestionGovernanceSnapshot();
        snapshot.setContractAllowlist(List.of("v2", "v1"));
        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setStage("CANARY");
        rollout.setTrafficPercent(100);
        rollout.setRollbackOnViolation(true);
        rollout.setMinSamples(2);
        rollout.setMaxRejectRate(0.2D);
        rollout.setRollbackAllowlist(List.of("v1"));
        snapshot.setRollout(rollout);

        GovernanceSyncApplyResult result = applier.apply(snapshot);

        assertThat(result.isRolledBack()).isTrue();
        assertThat(result.getStatus()).isEqualTo(GovernanceReleaseStatus.ROLLED_BACK);
        assertThat(result.getActiveVersions()).containsExactly("v1");
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v1");
    }

    @Test
    void shouldEnterRetryWaitAfterReleaseFailure() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        properties.setRequireCurrentVersionOverlap(true);
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );

        RemoteGovernanceRolloutPlan rollout = new RemoteGovernanceRolloutPlan();
        rollout.setReleaseId("r-1");
        rollout.setRetryBackoffSeconds(120);
        RemoteIngestionGovernanceSnapshot first = new RemoteIngestionGovernanceSnapshot();
        first.setContractAllowlist(List.of("v3"));
        first.setRollout(rollout);
        GovernanceSyncApplyResult failed = applier.apply(first);

        RemoteIngestionGovernanceSnapshot second = new RemoteIngestionGovernanceSnapshot();
        second.setContractAllowlist(List.of("v1", "v2"));
        second.setRollout(rollout);
        GovernanceSyncApplyResult waiting = applier.apply(second);

        assertThat(failed.getStatus()).isEqualTo(GovernanceReleaseStatus.FAILED);
        assertThat(waiting.getStatus()).isEqualTo(GovernanceReleaseStatus.RETRY_WAIT);
        assertThat(waiting.getNextRetryAtEpochMs()).isPositive();
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v1");
    }

    @Test
    void shouldEnterCooldownAfterRollback() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionContractHealthTracker healthTracker = new IngestionContractHealthTracker();
        healthTracker.recordContractRejected();
        healthTracker.recordContractRejected();
        healthTracker.recordAccepted();
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                healthTracker,
                "default"
        );

        RemoteGovernanceRolloutPlan rollbackRollout = new RemoteGovernanceRolloutPlan();
        rollbackRollout.setReleaseId("r-rollback");
        rollbackRollout.setStage("CANARY");
        rollbackRollout.setTrafficPercent(100);
        rollbackRollout.setMinSamples(2);
        rollbackRollout.setMaxRejectRate(0.2D);
        rollbackRollout.setRollbackAllowlist(List.of("v1"));
        rollbackRollout.setRollbackCooldownSeconds(120);

        RemoteIngestionGovernanceSnapshot first = new RemoteIngestionGovernanceSnapshot();
        first.setContractAllowlist(List.of("v2", "v1"));
        first.setRollout(rollbackRollout);
        GovernanceSyncApplyResult rolledBack = applier.apply(first);

        RemoteIngestionGovernanceSnapshot second = new RemoteIngestionGovernanceSnapshot();
        second.setContractAllowlist(List.of("v1", "v2"));
        second.setRollout(new RemoteGovernanceRolloutPlan());
        GovernanceSyncApplyResult cooling = applier.apply(second);

        assertThat(rolledBack.getStatus()).isEqualTo(GovernanceReleaseStatus.ROLLED_BACK);
        assertThat(cooling.getStatus()).isEqualTo(GovernanceReleaseStatus.COOLDOWN);
        assertThat(cooling.getNextRetryAtEpochMs()).isPositive();
    }

    @Test
    void shouldSkipLowerPriorityReleaseWhenAnotherReleaseActive() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(
                properties,
                guardService,
                null,
                new IngestionContractHealthTracker(),
                "default"
        );

        RemoteGovernanceRolloutPlan activeRollout = new RemoteGovernanceRolloutPlan();
        activeRollout.setReleaseId("r-high");
        activeRollout.setStage("CANARY");
        activeRollout.setPriority(10);
        activeRollout.setTrafficPercent(100);
        RemoteIngestionGovernanceSnapshot first = new RemoteIngestionGovernanceSnapshot();
        first.setContractAllowlist(List.of("v2", "v1"));
        first.setRollout(activeRollout);
        GovernanceSyncApplyResult running = applier.apply(first);

        RemoteGovernanceRolloutPlan incomingRollout = new RemoteGovernanceRolloutPlan();
        incomingRollout.setReleaseId("r-low");
        incomingRollout.setStage("CANARY");
        incomingRollout.setPriority(1);
        incomingRollout.setTrafficPercent(100);
        RemoteIngestionGovernanceSnapshot second = new RemoteIngestionGovernanceSnapshot();
        second.setContractAllowlist(List.of("v1", "v2"));
        second.setRollout(incomingRollout);
        GovernanceSyncApplyResult blocked = applier.apply(second);

        assertThat(running.getStatus()).isEqualTo(GovernanceReleaseStatus.RUNNING);
        assertThat(blocked.getStatus()).isEqualTo(GovernanceReleaseStatus.SKIPPED);
        assertThat(blocked.getReasonCode()).isEqualTo("RELEASE_CONFLICT");
    }
}
