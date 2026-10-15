package com.placementos.internship.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StartupValidator.class);

    @Test
    void shouldFailStartupWhenJwtSecretIsEmpty() {
        contextRunner
                .withPropertyValues(
                        "app.auth.jwt-secret=",
                        "spring.datasource.password=ValidPassword123"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("JWT secret");
                });
    }

    @Test
    void shouldFailStartupWhenJwtSecretIsShorterThan32Characters() {
        contextRunner
                .withPropertyValues(
                        "app.auth.jwt-secret=short_secret_key",
                        "spring.datasource.password=ValidPassword123"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("shorter than 32 characters");
                });
    }

    @Test
    void shouldFailStartupWhenDatabasePasswordIsMissingInNonTestProfile() {
        contextRunner
                .withPropertyValues(
                        "app.auth.jwt-secret=12345678901234567890123456789012",
                        "spring.datasource.password="
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Database password");
                });
    }

    @Test
    void shouldSucceedWhenValidCredentialsProvided() {
        contextRunner
                .withPropertyValues(
                        "app.auth.jwt-secret=12345678901234567890123456789012",
                        "spring.datasource.password=ValidPassword123"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    void shouldSucceedInTestProfileWithEmptyDatabasePassword() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("app.auth.jwt-secret", "12345678901234567890123456789012");
        env.setProperty("spring.datasource.password", "");
        env.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb");

        // Should not throw an exception
        StartupValidator.validateEnvironment(env);
    }

    @Test
    void shouldThrowWhenTestProfileHasShortJwtSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("app.auth.jwt-secret", "too_short");
        env.setProperty("spring.datasource.password", "");

        assertThatThrownBy(() -> StartupValidator.validateEnvironment(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shorter than 32 characters");
    }
}
