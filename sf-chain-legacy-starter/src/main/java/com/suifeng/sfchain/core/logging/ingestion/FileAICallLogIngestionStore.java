package com.suifeng.sfchain.core.logging.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
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
            if (properties.isIndexEnabled()) {
                Files.deleteIfExists(indexPath(file));
            }
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
        try {
            LineIndex index = loadOrBuildIndex(file);
            long offset = 0L;
            int checkpointLine = 0;
            if (index != null && !index.offsets.isEmpty()) {
                if (safeCursor >= index.totalLines) {
                    return new AICallLogIngestionPage(Collections.emptyList(), null, false);
                }
                int stride = Math.max(index.stride, 1);
                checkpointLine = (safeCursor / stride) * stride;
                int checkpointPos = Math.max(checkpointLine / stride, 0);
                if (checkpointPos >= index.offsets.size()) {
                    checkpointPos = index.offsets.size() - 1;
                    checkpointLine = checkpointPos * stride;
                }
                offset = index.offsets.get(checkpointPos);
            }
            int skip = safeCursor - checkpointLine;
            return readPage(file, offset, skip, safeLimit, safeCursor);
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
                if (!Files.isRegularFile(file) || !isDataFile(file)) {
                    continue;
                }
                FileTime time = Files.getLastModifiedTime(file);
                if (time.toInstant().isBefore(cutoff)) {
                    Files.deleteIfExists(file);
                    Files.deleteIfExists(indexPath(file));
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

    private AICallLogIngestionPage readPage(
            Path file,
            long offset,
            int skipLines,
            int limit,
            int cursor) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            channel.position(Math.max(offset, 0L));
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(Channels.newInputStream(channel), StandardCharsets.UTF_8))) {
                for (int i = 0; i < skipLines; i++) {
                    if (reader.readLine() == null) {
                        return new AICallLogIngestionPage(Collections.emptyList(), null, false);
                    }
                }
                List<AICallLogIngestionRecord> page = new ArrayList<>(limit);
                boolean hasMore = false;
                for (int i = 0; i < limit + 1; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    AICallLogIngestionRecord record = toRecord(line);
                    if (record == null) {
                        continue;
                    }
                    if (page.size() < limit) {
                        page.add(record);
                    } else {
                        hasMore = true;
                        break;
                    }
                }
                Integer nextCursor = hasMore ? cursor + page.size() : null;
                return new AICallLogIngestionPage(page, nextCursor, hasMore);
            }
        }
    }

    private LineIndex loadOrBuildIndex(Path file) throws IOException {
        if (!properties.isIndexEnabled()) {
            return null;
        }
        Path idxPath = indexPath(file);
        long fileSize = Files.size(file);
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        if (Files.exists(idxPath)) {
            try {
                LineIndex loaded = objectMapper.readValue(Files.readString(idxPath, StandardCharsets.UTF_8), LineIndex.class);
                if (loaded != null
                        && loaded.fileSize == fileSize
                        && loaded.lastModifiedMillis == lastModified
                        && loaded.stride == Math.max(properties.getIndexStride(), 1)) {
                    return loaded;
                }
            } catch (Exception ignored) {
            }
        }
        LineIndex built = buildIndex(file, Math.max(properties.getIndexStride(), 1), fileSize, lastModified);
        Files.writeString(idxPath, objectMapper.writeValueAsString(built), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return built;
    }

    private static LineIndex buildIndex(Path file, int stride, long fileSize, long lastModifiedMillis) throws IOException {
        List<Long> offsets = new ArrayList<>();
        int totalLines = 0;
        long offset = 0L;
        boolean hasData = false;
        int prev = -1;
        offsets.add(0L);
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file, StandardOpenOption.READ))) {
            int b;
            while ((b = inputStream.read()) != -1) {
                hasData = true;
                if (b == '\n') {
                    totalLines++;
                    if (totalLines % stride == 0) {
                        offsets.add(offset + 1);
                    }
                }
                prev = b;
                offset++;
            }
        }
        if (hasData && prev != '\n') {
            totalLines++;
        }
        return new LineIndex(fileSize, lastModifiedMillis, stride, totalLines, offsets);
    }

    private static Path indexPath(Path dataFile) {
        return dataFile.resolveSibling(dataFile.getFileName() + ".idx.json");
    }

    private static boolean isDataFile(Path path) {
        return path.getFileName().toString().endsWith(".jsonl");
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

    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Data
    private static class LineIndex {
        private long fileSize;
        private long lastModifiedMillis;
        private int stride;
        private int totalLines;
        private List<Long> offsets;
    }
}
