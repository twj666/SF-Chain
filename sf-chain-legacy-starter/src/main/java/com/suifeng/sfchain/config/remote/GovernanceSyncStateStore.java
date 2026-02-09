package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 治理同步状态存储
 */
@Slf4j
@RequiredArgsConstructor
public class GovernanceSyncStateStore {

    private final ObjectMapper objectMapper;
    private final String filePath;

    public Optional<GovernanceSyncRuntimeState> load() {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return Optional.empty();
            }
            GovernanceSyncRuntimeState state =
                    objectMapper.readValue(Files.readString(path), GovernanceSyncRuntimeState.class);
            return Optional.ofNullable(state);
        } catch (Exception ex) {
            log.warn("加载治理同步状态失败: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void save(GovernanceSyncRuntimeState state) {
        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, objectMapper.writeValueAsString(state));
        } catch (Exception ex) {
            log.warn("保存治理同步状态失败: {}", ex.getMessage());
        }
    }
}
