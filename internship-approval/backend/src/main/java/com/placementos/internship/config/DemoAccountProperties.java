package com.placementos.internship.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.demo")
public class DemoAccountProperties {

    private boolean enabled = true;
    private List<DemoAccountConfig> accounts = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<DemoAccountConfig> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<DemoAccountConfig> accounts) {
        this.accounts = accounts;
    }

    public static class DemoAccountConfig {
        private String email;
        private String password;
        private String name;
        private String role;
        private String collegeId;
        private Double cgpa;
        private String branch;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getCollegeId() { return collegeId; }
        public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

        public Double getCgpa() { return cgpa; }
        public void setCgpa(Double cgpa) { this.cgpa = cgpa; }

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
    }
}
