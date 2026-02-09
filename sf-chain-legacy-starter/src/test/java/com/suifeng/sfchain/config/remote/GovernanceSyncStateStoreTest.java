package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceSyncStateStoreTest {

    @Test
    void shouldPersistAndLoadRuntimeState() throws Exception {
        Path file = Files.createTempFile("sf-chain-gov-state", ".json");
        GovernanceSyncStateStore store = new GovernanceSyncStateStore(new ObjectMapper(), file.toString());
        GovernanceSyncRuntimeState state = new GovernanceSyncRuntimeState();
        GovernanceFinalizeRecord record = new GovernanceFinalizeRecord();
        record.setStatus(GovernanceReleaseStatus.SUCCEEDED);
        record.setUpdatedAtEpochMs(System.currentTimeMillis());
        state.getFinalizedStates().put("release-1", record);

        store.save(state);
        GovernanceSyncRuntimeState loaded = store.load().orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getFinalizedStates()).containsKey("release-1");
        assertThat(loaded.getFinalizedStates().get("release-1").getStatus())
                .isEqualTo(GovernanceReleaseStatus.SUCCEEDED);

        Files.deleteIfExists(file);
    }
}
