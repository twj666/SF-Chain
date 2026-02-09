package com.suifeng.sfchain.core.logging.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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

    @Test
    void shouldQueryLastRecords() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setFilePersistenceDir(tempDir.toString());
        FileAICallLogIngestionStore store = new FileAICallLogIngestionStore(new ObjectMapper(), properties);

        store.saveBatch("tenant-a", "app-a", List.of(sampleItem("call-1")));
        store.saveBatch("tenant-a", "app-a", List.of(sampleItem("call-2")));

        List<AICallLogIngestionRecord> records = store.query("tenant-a", "app-a", 1);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getItem().getCallId()).isEqualTo("call-2");
    }

    @Test
    void shouldPurgeExpiredFiles() throws Exception {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setFilePersistenceDir(tempDir.toString());
        properties.setRetentionDays(1);
        FileAICallLogIngestionStore store = new FileAICallLogIngestionStore(new ObjectMapper(), properties);

        store.saveBatch("tenant-a", "app-a", List.of(sampleItem("call-old")));
        Path file = tempDir.resolve("tenant-a__app-a.jsonl");
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minusSeconds(3 * 24 * 3600L)));

        int deleted = store.purgeExpired();
        assertThat(deleted).isEqualTo(1);
        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void shouldQueryByCursor() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setFilePersistenceDir(tempDir.toString());
        properties.setIndexEnabled(true);
        properties.setIndexStride(1);
        FileAICallLogIngestionStore store = new FileAICallLogIngestionStore(new ObjectMapper(), properties);

        store.saveBatch("tenant-a", "app-a", List.of(sampleItem("call-1"), sampleItem("call-2"), sampleItem("call-3")));

        AICallLogIngestionPage page = store.queryPage("tenant-a", "app-a", 0, 2);
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getNextCursor()).isEqualTo(2);
        assertThat(Files.exists(tempDir.resolve("tenant-a__app-a.jsonl.idx.json"))).isTrue();
    }

    @Test
    void shouldRebuildIndexesInBatch() {
        SfChainIngestionProperties properties = new SfChainIngestionProperties();
        properties.setFilePersistenceDir(tempDir.toString());
        properties.setIndexEnabled(true);
        properties.setIndexStride(1);
        FileAICallLogIngestionStore store = new FileAICallLogIngestionStore(new ObjectMapper(), properties);

        store.saveBatch("tenant-a", "app-a", List.of(sampleItem("call-a")));
        store.saveBatch("tenant-b", "app-b", List.of(sampleItem("call-b")));

        int rebuilt = store.rebuildIndexes();
        assertThat(rebuilt).isEqualTo(2);
        assertThat(Files.exists(tempDir.resolve("tenant-a__app-a.jsonl.idx.json"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("tenant-b__app-b.jsonl.idx.json"))).isTrue();
    }

    private static AICallLogUploadItem sampleItem(String callId) {
        return AICallLogUploadItem.builder()
                .callId(callId)
                .operationType("PERSIST_TEST")
                .modelName("test-model")
                .callTime(LocalDateTime.now())
                .status("SUCCESS")
                .duration(7L)
                .build();
    }
}
