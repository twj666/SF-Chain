package com.suifeng.sfchain.core.logging.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileAICallLogIngestionStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAppendBatchAsJsonLines() throws Exception {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setFilePersistenceDir(tempDir.toString());
        FileAICallLogIngestionStore store = new FileAICallLogIngestionStore(new ObjectMapper(), properties);

        AICallLogUploadItem item = AICallLogUploadItem.builder()
                .callId("call-persist-1")
                .operationType("PERSIST_TEST")
                .modelName("test-model")
                .callTime(LocalDateTime.now())
                .status("SUCCESS")
                .duration(7L)
                .build();

        store.saveBatch("tenant-a", "app-a", List.of(item));

        Path file = tempDir.resolve("tenant-a__app-a.jsonl");
        assertThat(Files.exists(file)).isTrue();
        String content = Files.readString(file);
        assertThat(content).contains("call-persist-1");
        assertThat(content).contains("tenant-a");
        assertThat(content).contains("app-a");
    }
}
