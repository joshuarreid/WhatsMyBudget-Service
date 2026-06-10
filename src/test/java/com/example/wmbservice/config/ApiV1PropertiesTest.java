package com.example.wmbservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ApiV1PropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void defaultsToActiveWhenPropertyIsMissing() {
        contextRunner.run(context -> {
            assertThat(context.getStartupFailure()).isNull();
            ApiV1Properties properties = context.getBean(ApiV1Properties.class);
            assertThat(properties.getMode()).isEqualTo(ApiV1Properties.Mode.ACTIVE);
        });
    }

    @Test
    void bindsExplicitModeValue() {
        contextRunner
                .withPropertyValues("api.v1.mode=disabled")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    ApiV1Properties properties = context.getBean(ApiV1Properties.class);
                    assertThat(properties.getMode()).isEqualTo(ApiV1Properties.Mode.DISABLED);
                });
    }

    @Test
    void failsFastOnInvalidMode() {
        contextRunner
                .withPropertyValues("api.v1.mode=not-a-real-mode")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure().getCause()).isNotNull();
                    assertThat(context.getStartupFailure().getCause().getMessage()).contains("api.v1.mode");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApiV1Properties.class)
    static class TestConfig {
    }
}
