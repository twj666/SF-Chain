package com.suifeng.sfchain.configcenter.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class BootstrapDbPropertyResolver {

    private static final Path STORE_PATH = Paths.get("data", "bootstrap-db.properties");

    private BootstrapDbPropertyResolver() {
    }

    public static void applyIfPresent() {
        if (!Files.exists(STORE_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(STORE_PATH)) {
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load bootstrap db config", ex);
        }

        String dbType = value(properties, "databaseType");
        String jdbcUrl = value(properties, "jdbcUrl");
        String username = value(properties, "username");
        String password = properties.getProperty("password", "");

        if (isBlank(dbType) || isBlank(jdbcUrl) || isBlank(username)) {
            return;
        }

        String driverClass = resolveDriverClass(dbType, jdbcUrl);
        if (isBlank(driverClass)) {
            return;
        }

        System.setProperty("SF_CHAIN_DB_TYPE", dbType);
        System.setProperty("SF_CHAIN_DB_URL", jdbcUrl);
        System.setProperty("SF_CHAIN_DB_USERNAME", username);
        System.setProperty("SF_CHAIN_DB_PASSWORD", password);
        System.setProperty("SF_CHAIN_DB_DRIVER", driverClass);
    }

    private static String value(Properties properties, String key) {
        return properties.getProperty(key);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String resolveDriverClass(String databaseType, String jdbcUrl) {
        String normalizedUrl = jdbcUrl.trim().toLowerCase();
        if (normalizedUrl.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if (normalizedUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        if (normalizedUrl.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }

        return driverClassByType(databaseType);
    }

    private static String driverClassByType(String databaseType) {
        String normalized = databaseType.trim().toLowerCase();
        if ("mysql".equals(normalized)) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if ("postgresql".equals(normalized)) {
            return "org.postgresql.Driver";
        }
        return null;
    }
}
