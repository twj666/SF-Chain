package com.suifeng.sfchain.config.remote;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.ingestion.ContractAllowlistGuardService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionGovernanceSyncApplierTest {

    @Test
    void shouldApplyAllowlistWhenGuardValidationPasses() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        ContractAllowlistGuardService guardService = new ContractAllowlistGuardService(properties);
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(properties, guardService, null);

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
        IngestionGovernanceSyncApplier applier = new IngestionGovernanceSyncApplier(properties, guardService, null);

        RemoteIngestionGovernanceSnapshot snapshot = new RemoteIngestionGovernanceSnapshot();
        snapshot.setContractAllowlist(List.of("v3"));

        GovernanceSyncApplyResult result = applier.apply(snapshot);

        assertThat(result.isValid()).isFalse();
        assertThat(result.isApplied()).isFalse();
        assertThat(properties.getSupportedContractVersion()).isEqualTo("v1");
    }
}
