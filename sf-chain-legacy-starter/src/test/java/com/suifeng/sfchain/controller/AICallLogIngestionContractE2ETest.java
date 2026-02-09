package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainIngestionProperties;
import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.ingestion.AICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.FileAICallLogIngestionStore;
import com.suifeng.sfchain.core.logging.ingestion.MinuteWindowQuotaService;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadItem;
import com.suifeng.sfchain.core.logging.upload.HttpAICallLogUploadClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AICallLogIngestionContractE2ETest.TestConfig.class
)
@ContextConfiguration(initializers = AICallLogIngestionContractE2ETest.PropertyInitializer.class)
class AICallLogIngestionContractE2ETest {

    private static final Path TEMP_DIR = createTempDir();

    @LocalServerPort
    private int port;

    @Autowired
    private SfChainIngestionProperties ingestionProperties;

    @Test
    void shouldUploadToIngestionEndpointAndPersist() throws Exception {
        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://127.0.0.1:" + port);
        serverProperties.setApiKey("center-key");
        serverProperties.setTenantId("tenant-e2e");
        serverProperties.setAppId("app-e2e");

        SfChainLoggingProperties loggingProperties = new SfChainLoggingProperties();
        loggingProperties.setUploadEndpoint("/v1/logs/ai-calls/batch");

        HttpAICallLogUploadClient client = new HttpAICallLogUploadClient(
                new ObjectMapper(),
                serverProperties,
                loggingProperties
        );
        AICallLogUploadItem item = AICallLogUploadItem.builder()
                .callId("call-e2e-1")
                .operationType("E2E_OP")
                .modelName("e2e-model")
                .callTime(LocalDateTime.now())
                .duration(11L)
                .status("SUCCESS")
                .build();

        boolean ok = client.upload(List.of(item));
        assertThat(ok).isTrue();

        Path file = Path.of(ingestionProperties.getFilePersistenceDir())
                .resolve("tenant-e2e__app-e2e.jsonl");
        assertThat(Files.exists(file)).isTrue();
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("call-e2e-1");
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (!Files.exists(TEMP_DIR)) {
            return;
        }
        try (var walk = Files.walk(TEMP_DIR)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    static class PropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "sf-chain.ingestion.api-key=center-key",
                    "sf-chain.ingestion.require-tenant-app=true",
                    "sf-chain.ingestion.file-persistence-enabled=true",
                    "sf-chain.ingestion.file-persistence-dir=" + TEMP_DIR,
                    "spring.autoconfigure.exclude="
                            + DataSourceAutoConfiguration.class.getName() + ","
                            + DataSourceTransactionManagerAutoConfiguration.class.getName() + ","
                            + JdbcTemplateAutoConfiguration.class.getName() + ","
                            + HibernateJpaAutoConfiguration.class.getName()
            ).applyTo(context.getEnvironment());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties({SfChainIngestionProperties.class, SfChainLoggingProperties.class})
    @Import(AICallLogIngestionController.class)
    static class TestConfig {
        @Bean
        AICallLogManager aiCallLogManager(SfChainLoggingProperties properties) {
            return new AICallLogManager(properties);
        }

        @Bean
        MinuteWindowQuotaService minuteWindowQuotaService(SfChainIngestionProperties properties) {
            return new MinuteWindowQuotaService(properties);
        }

        @Bean
        AICallLogIngestionStore aiCallLogIngestionStore(
                ObjectMapper objectMapper,
                SfChainIngestionProperties properties) {
            return new FileAICallLogIngestionStore(objectMapper, properties);
        }
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("sf-chain-e2e-");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
