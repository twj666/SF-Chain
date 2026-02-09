package com.suifeng.sfchain.configcenter.bootstrap;

import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Properties;

@Service
public class DatabaseBootstrapService {

    private static final Path STORE_PATH = Paths.get("data", "bootstrap-db.properties");

    public DatabaseStatus status() {
        DatabaseStatus status = new DatabaseStatus();
        status.setConfigSaved(Files.exists(STORE_PATH));
        if (!status.isConfigSaved()) {
            return status;
        }
        Properties properties = load();
        status.setDatabaseType(properties.getProperty("databaseType"));
        status.setJdbcUrl(properties.getProperty("jdbcUrl"));
        status.setSavedAt(properties.getProperty("savedAt"));
        return status;
    }

    public void testConnection(DatabaseRequest request) {
        validate(request);
        try (Connection ignored = DriverManager.getConnection(request.getJdbcUrl(), request.getUsername(), request.getPassword())) {
            // ok
        } catch (Exception ex) {
            throw new IllegalArgumentException("database connection failed: " + ex.getMessage(), ex);
        }
    }

    public InitResult initialize(DatabaseRequest request) {
        validate(request);
        String script = migrationScript(request.getDatabaseType());
        try (Connection connection = DriverManager.getConnection(request.getJdbcUrl(), request.getUsername(), request.getPassword())) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(script));
            save(request);
            InitResult result = new InitResult();
            result.setSuccess(true);
            result.setMessage("database initialized successfully");
            result.setRestartRequired(true);
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("database initialization failed: " + ex.getMessage(), ex);
        }
    }

    private static void validate(DatabaseRequest request) {
        if (isBlank(request.getDatabaseType()) || isBlank(request.getJdbcUrl()) || isBlank(request.getUsername())) {
            throw new IllegalArgumentException("databaseType/jdbcUrl/username are required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String migrationScript(String databaseType) {
        String type = databaseType.trim().toLowerCase();
        if ("postgresql".equals(type)) {
            return "db/bootstrap/v2_postgresql.sql";
        }
        if ("mysql".equals(type)) {
            return "db/bootstrap/v2_mysql.sql";
        }
        throw new IllegalArgumentException("unsupported databaseType: " + databaseType);
    }

    private static void save(DatabaseRequest request) {
        Properties properties = new Properties();
        properties.setProperty("databaseType", request.getDatabaseType());
        properties.setProperty("jdbcUrl", request.getJdbcUrl());
        properties.setProperty("username", request.getUsername());
        properties.setProperty("password", request.getPassword() == null ? "" : request.getPassword());
        properties.setProperty("savedAt", Instant.now().toString());
        try {
            Files.createDirectories(STORE_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(STORE_PATH)) {
                properties.store(output, "SF-Chain Config Center Database Bootstrap");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("failed to save bootstrap db config", ex);
        }
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(STORE_PATH)) {
            properties.load(input);
            return properties;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load bootstrap db config", ex);
        }
    }

    @Data
    public static class DatabaseRequest {
        private String databaseType;
        private String jdbcUrl;
        private String username;
        private String password;
    }

    @Data
    public static class InitResult {
        private boolean success;
        private String message;
        private boolean restartRequired;
    }

    @Data
    public static class DatabaseStatus {
        private boolean configSaved;
        private String databaseType;
        private String jdbcUrl;
        private String savedAt;
    }
}
