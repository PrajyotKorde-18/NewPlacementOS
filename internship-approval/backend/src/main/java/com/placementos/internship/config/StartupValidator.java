package com.placementos.internship.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StartupValidator implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        validateEnvironment(this.environment);
    }

    public static void validateEnvironment(Environment env) {
        if (env == null) {
            return;
        }

        // 1. Validate JWT Secret: must not be empty and must be at least 32 characters
        String jwtSecret = env.getProperty("app.auth.jwt-secret");
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            jwtSecret = env.getProperty("JWT_SECRET");
        }
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("Startup failed: JWT secret (JWT_SECRET / app.auth.jwt-secret) is missing or empty.");
        }
        if (jwtSecret.trim().length() < 32) {
            throw new IllegalStateException("Startup failed: JWT secret (JWT_SECRET / app.auth.jwt-secret) is shorter than 32 characters (length: " + jwtSecret.trim().length() + ").");
        }

        // 2. Validate Database Password: required in non-test profiles
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        String datasourceUrl = env.getProperty("spring.datasource.url");
        boolean isTest = activeProfiles.contains("test") || (datasourceUrl != null && datasourceUrl.contains("h2:mem"));

        if (!isTest) {
            String dbPassword = env.getProperty("spring.datasource.password");
            if (dbPassword == null || dbPassword.trim().isEmpty()) {
                dbPassword = env.getProperty("SPRING_DATASOURCE_PASSWORD");
            }
            if (dbPassword == null || dbPassword.trim().isEmpty()) {
                throw new IllegalStateException("Startup failed: Database password (SPRING_DATASOURCE_PASSWORD / spring.datasource.password) is missing or empty.");
            }
        }
    }
}
