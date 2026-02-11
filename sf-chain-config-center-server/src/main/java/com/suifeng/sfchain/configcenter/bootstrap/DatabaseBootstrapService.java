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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Service
public class DatabaseBootstrapService {

    private static final Path STORE_PATH = Paths.get("data", "bootstrap-db.properties");
    private static final List<String> CONTROL_PLANE_TABLES = List.of(
            "sfchain_cp_tenants",
            "sfchain_cp_apps",
            "sfchain_cp_api_keys",
            "sfchain_cp_model_configs",
            "sfchain_cp_operation_configs",
            "sfchain_cp_config_releases",
            "sfchain_cp_call_logs"
    );

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

    public void saveConfig(DatabaseRequest request) {
        validate(request);
        save(request);
    }

    public PrecheckResult precheck(DatabaseRequest request) {
        validate(request);
        try (Connection connection = DriverManager.getConnection(request.getJdbcUrl(), request.getUsername(), request.getPassword())) {
            return buildPrecheck(connection);
        } catch (Exception ex) {
            throw new IllegalArgumentException("database precheck failed: " + ex.getMessage(), ex);
        }
    }

    public InitResult initialize(DatabaseRequest request) {
        validate(request);
        String script = migrationScript(request.getDatabaseType());
        try (Connection connection = DriverManager.getConnection(request.getJdbcUrl(), request.getUsername(), request.getPassword())) {
            PrecheckResult precheck = buildPrecheck(connection);
            if (!precheck.isSafeToInitialize()) {
                InitResult result = new InitResult();
                result.setSuccess(false);
                result.setRestartRequired(false);
                result.setPrecheck(precheck);
                if (precheck.isHasNonEmptyTables()) {
                    result.setMessage("initialization blocked: existing control-plane tables already contain data");
                    return result;
                }
                if (!Boolean.TRUE.equals(request.getForce())) {
                    result.setMessage("initialization blocked: control-plane tables already exist, pass force=true to continue");
                    return result;
                }
            }

            ScriptUtils.executeSqlScript(connection, new ClassPathResource(script));
            save(request);
            InitResult result = new InitResult();
            result.setSuccess(true);
            result.setMessage("database initialized successfully");
            result.setRestartRequired(true);
            result.setPrecheck(precheck);
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("database initialization failed: " + ex.getMessage(), ex);
        }
    }

    private static void validate(DatabaseRequest request) {
        if (isBlank(request.getDatabaseType()) || isBlank(request.getJdbcUrl()) || isBlank(request.getUsername())) {
            throw new IllegalArgumentException("databaseType/jdbcUrl/username are required");
        }
        String type = request.getDatabaseType().trim().toLowerCase();
        String url = request.getJdbcUrl().trim().toLowerCase();
        if ("mysql".equals(type) && !url.startsWith("jdbc:mysql:")) {
            throw new IllegalArgumentException("mysql jdbcUrl must start with jdbc:mysql:");
        }
        if ("postgresql".equals(type) && !url.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("postgresql jdbcUrl must start with jdbc:postgresql:");
        }
        if (!"mysql".equals(type) && !"postgresql".equals(type)) {
            throw new IllegalArgumentException("unsupported databaseType: " + request.getDatabaseType());
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

    private static PrecheckResult buildPrecheck(Connection connection) throws Exception {
        List<String> existingTables = new ArrayList<>();
        List<String> nonEmptyTables = new ArrayList<>();
        for (String table : CONTROL_PLANE_TABLES) {
            if (!tableExists(connection, table)) {
                continue;
            }
            existingTables.add(table);
            if (rowCount(connection, table) > 0) {
                nonEmptyTables.add(table);
            }
        }

        PrecheckResult result = new PrecheckResult();
        result.setExistingTables(existingTables);
        result.setNonEmptyTables(nonEmptyTables);
        result.setHasExistingTables(!existingTables.isEmpty());
        result.setHasNonEmptyTables(!nonEmptyTables.isEmpty());
        result.setRequiresForce(result.isHasExistingTables() && !result.isHasNonEmptyTables());
        result.setSafeToInitialize(!result.isHasExistingTables());
        return result;
    }

    private static boolean tableExists(Connection connection, String table) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, table.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static long rowCount(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
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
        private Boolean force;
    }

    @Data
    public static class InitResult {
        private boolean success;
        private String message;
        private boolean restartRequired;
        private PrecheckResult precheck;
    }

    @Data
    public static class PrecheckResult {
        private boolean hasExistingTables;
        private boolean hasNonEmptyTables;
        private boolean requiresForce;
        private boolean safeToInitialize;
        private List<String> existingTables;
        private List<String> nonEmptyTables;
    }

    @Data
    public static class DatabaseStatus {
        private boolean configSaved;
        private String databaseType;
        private String jdbcUrl;
        private String savedAt;
    }
}
