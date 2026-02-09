package com.suifeng.sfchain.config;

import com.suifeng.sfchain.controller.AICallLogController;
import com.suifeng.sfchain.controller.AIModelController;
import com.suifeng.sfchain.controller.IndexController;
import com.suifeng.sfchain.core.AIOperationRegistry;
import com.suifeng.sfchain.core.AIService;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.persistence.PersistenceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SfChainAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SfChainAutoConfiguration.class,
                    OpenAIAutoConfiguration.class,
                    SfChainPersistenceAutoConfiguration.class,
                    SfChainManagementAutoConfiguration.class,
                    SfChainPersistenceManagementAutoConfiguration.class,
                    SfChainStaticUiAutoConfiguration.class
            ))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void shouldLoadCoreBeansAndDisableHeavyFeaturesByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AIService.class);
            assertThat(context).hasSingleBean(AIOperationRegistry.class);
            assertThat(context).hasSingleBean(AICallLogManager.class);

            assertThat(context).doesNotHaveBean(PersistenceManager.class);
            assertThat(context).doesNotHaveBean(AICallLogController.class);
            assertThat(context).doesNotHaveBean(AIModelController.class);
            assertThat(context).doesNotHaveBean(IndexController.class);
        });
    }

    @Test
    void shouldLoadManagementApiWithoutPersistenceControllers() {
        contextRunner
                .withPropertyValues("sf-chain.features.management-api=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AICallLogController.class);
                    assertThat(context).doesNotHaveBean(AIModelController.class);
                    assertThat(context).doesNotHaveBean(PersistenceManager.class);
                });
    }

    @Test
    void shouldLoadStaticUiOnlyWhenEnabled() {
        contextRunner
                .withPropertyValues("sf-chain.features.static-ui=true")
                .run(context -> assertThat(context).hasSingleBean(IndexController.class));
    }
}
