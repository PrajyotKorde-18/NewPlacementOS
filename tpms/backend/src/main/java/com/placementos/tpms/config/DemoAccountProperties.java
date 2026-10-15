package com.placementos.tpms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.demo")
@Data
public class DemoAccountProperties {

    private boolean enabled = true;
    private List<DemoAccountConfig> accounts = new ArrayList<>();

    @Data
    public static class DemoAccountConfig {
        private String email;
        private String password;
        private String name;
        private String role;
        private String collegeId;
        private String collegeRollNumber;
        private String phoneNumber;
        private String branch;
        private Double cgpa;
        private Double tenthPercentage;
        private Double twelfthPercentage;
        private String skills;
    }
}
