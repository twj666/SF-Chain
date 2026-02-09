package com.suifeng.sfchain.core.logging.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接入日志文件持久化（jsonl）
 */
@Slf4j
public class FileAICallLogIngestionStore implements AICallLogIngestionStore {

    private final ObjectMapper objectMapper;
    private final SfChainIngestionProperties properties;
    private final Path baseDir;

    public FileAICallLogIngestionStore(ObjectMapper objectMapper, SfChainIngestionProperties properties) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.properties = properties;
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
                lines.add(objectMapper.writeValueAsString(new AICallLogIngestionRecord(tenantId, appId, now, item)));
            }
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 持久化失败不影响主流程
            log.debug("持久化接入日志失败: {}", e.getMessage());
        }
    }

    @Override
    public List<AICallLogIngestionRecord> query(String tenantId, String appId, int limit) {
        Path file = baseDir.resolve(toFileName(tenantId, appId));
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        try {
            List<String> lines = readLastLines(file, Math.max(limit, 1));
            return lines.stream()
                    .map(this::toRecord)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.debug("查询接入日志失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public AICallLogIngestionPage queryPage(String tenantId, String appId, int cursor, int limit) {
        Path file = baseDir.resolve(toFileName(tenantId, appId));
        if (!Files.exists(file)) {
            return new AICallLogIngestionPage(Collections.emptyList(), null, false);
        }
        int safeCursor = Math.max(cursor, 0);
        int safeLimit = Math.max(limit, 1);
        try (var stream = Files.lines(file, StandardCharsets.UTF_8)) {
            List<AICallLogIngestionRecord> all = stream
                    .skip(safeCursor)
                    .limit((long) safeLimit + 1)
                    .map(this::toRecord)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            boolean hasMore = all.size() > safeLimit;
            List<AICallLogIngestionRecord> page = hasMore ? all.subList(0, safeLimit) : all;
            Integer nextCursor = hasMore ? safeCursor + safeLimit : null;
            return new AICallLogIngestionPage(page, nextCursor, hasMore);
        } catch (IOException e) {
            log.debug("分页查询接入日志失败: {}", e.getMessage());
            return new AICallLogIngestionPage(Collections.emptyList(), null, false);
        }
    }

    @Override
    public int purgeExpired() {
        if (!Files.exists(baseDir)) {
            return 0;
        }
        int deleted = 0;
        long retentionDays = Math.max(properties.getRetentionDays(), 1);
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 24L * 3600L);
        try (var stream = Files.list(baseDir)) {
            for (Path file : stream.collect(Collectors.toList())) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                FileTime time = Files.getLastModifiedTime(file);
                if (time.toInstant().isBefore(cutoff)) {
                    Files.deleteIfExists(file);
                    deleted++;
                }
            }
        } catch (IOException e) {
            log.debug("清理过期接入日志失败: {}", e.getMessage());
        }
        return deleted;
    }

    private AICallLogIngestionRecord toRecord(String line) {
        try {
            return objectMapper.readValue(line, AICallLogIngestionRecord.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> readLastLines(Path file, int limit) throws IOException {
        Deque<String> queue = new ArrayDeque<>(limit);
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (queue.size() == limit) {
                    queue.removeFirst();
                }
                queue.addLast(line);
            }
        }
        return new ArrayList<>(queue);
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
}
