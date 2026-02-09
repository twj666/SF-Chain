package com.suifeng.sfchain.core.logging.ingestion;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractAllowlistGuardServiceTest {

    @Test
    void shouldRejectWhenProposedVersionExceedsLimit() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        properties.setMaxActiveContractVersions(2);
        ContractAllowlistGuardService service = new ContractAllowlistGuardService(properties);

        ContractAllowlistGuardService.ValidationResult result = service.validate(List.of("v1", "v2", "v3"));

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldRejectWhenNoOverlapRequired() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        properties.setRequireCurrentVersionOverlap(true);
        ContractAllowlistGuardService service = new ContractAllowlistGuardService(properties);

        ContractAllowlistGuardService.ValidationResult result = service.validate(List.of("v2"));

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldAcceptCompatibleVersionWindow() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setSupportedContractVersion("v1");
        properties.setRequireCurrentVersionOverlap(true);
        properties.setMaxActiveContractVersions(2);
        ContractAllowlistGuardService service = new ContractAllowlistGuardService(properties);

        ContractAllowlistGuardService.ValidationResult result = service.validate(List.of("v1", "v2"));

        assertThat(result.isValid()).isTrue();
    }
}
