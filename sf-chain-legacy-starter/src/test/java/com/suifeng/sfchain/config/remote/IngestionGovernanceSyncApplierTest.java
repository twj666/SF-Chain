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
        assertThat(result.getActiveVersions()).containsExactly("v1");
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v1");
    }
}
