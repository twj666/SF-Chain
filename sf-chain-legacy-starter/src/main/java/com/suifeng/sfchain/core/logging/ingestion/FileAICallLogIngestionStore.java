package com.suifeng.sfchain.core.logging.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 接入日志文件持久化（jsonl）
 */
public class FileAICallLogIngestionStore implements AICallLogIngestionStore {

    private final ObjectMapper objectMapper;
    private final Path baseDir;

    public FileAICallLogIngestionStore(ObjectMapper objectMapper, SfChainIngestionProperties properties) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.baseDir = Path.of(properties.getFilePersistenceDir()).toAbsolutePath().normalize();
    }

    @Override
    public synchronized void saveBatch(String tenantId, String appId, List<AICallLogUploadItem> items) {
        try {
            Files.createDirectories(baseDir);
            Path file = baseDir.resolve(toFileName(tenantId, appId));
            List<String> lines = new ArrayList<>(items.size());
            LocalDateTime now = LocalDateTime.now();
            for (AICallLogUploadItem item : items) {
                lines.add(objectMapper.writeValueAsString(new IngestionRecord(tenantId, appId, now, item)));
            }
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // 持久化失败不影响主流程
        }
    }

    private static String toFileName(String tenantId, String appId) {
        String tenant = sanitize(tenantId);
        String app = sanitize(appId);
        return tenant + "__" + app + ".jsonl";
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @lombok.Value
    private static class IngestionRecord {
        String tenantId;
        String appId;
        LocalDateTime ingestedAt;
        AICallLogUploadItem item;
    }
}
