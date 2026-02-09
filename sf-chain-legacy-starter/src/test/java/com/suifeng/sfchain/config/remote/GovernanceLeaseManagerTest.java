package com.suifeng.sfchain.config.remote;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceLeaseManagerTest {

    @Test
    void shouldAllowOnlyOneHolderForSameLockFile() throws Exception {
        Path lockFile = Files.createTempFile("sf-chain-lease", ".lock");
        GovernanceLeaseManager first = new GovernanceLeaseManager(
                true,
                false,
                30,
                "owner-1",
                null,
                lockFile.toString()
        );
        GovernanceLeaseManager second = new GovernanceLeaseManager(
                true,
                false,
                30,
                "owner-2",
                null,
                lockFile.toString()
        );

        boolean firstAcquired = first.tryAcquire();
        boolean secondAcquired = second.tryAcquire();

        assertThat(firstAcquired).isTrue();
        assertThat(secondAcquired).isFalse();

        first.release();
        second.release();
        Files.deleteIfExists(lockFile);
    }
}
