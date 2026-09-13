package com.placementos.internship.config;

import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-name:System CDPC Admin}")
    private String adminName;

    @Value("${app.bootstrap.admin-college-id:ADMIN001}")
    private String adminCollegeId;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.CDPC_ADMIN)) {
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.warn("[BOOTSTRAP WARNING] No CDPC_ADMIN found in database, but environment variables BOOTSTRAP_ADMIN_EMAIL and/or BOOTSTRAP_ADMIN_PASSWORD are not provided. Skipping admin seed. Provide BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD env vars on deployment to enable automated admin seeding.");
                return;
            }

            log.info("[BOOTSTRAP] No CDPC_ADMIN found in system. Seeding initial bootstrap CDPC Admin account from environment configuration...");
            User admin = User.builder()
                    .email(adminEmail.trim())
                    .password(passwordEncoder.encode(adminPassword.trim()))
                    .name(adminName.trim())
                    .role(Role.CDPC_ADMIN)
                    .collegeId(adminCollegeId.trim())
                    .build();

            userRepository.save(admin);
            log.info("[BOOTSTRAP] Initial CDPC_ADMIN account created successfully with email: '{}'.", adminEmail.trim());
        } else {
            log.info("[BOOTSTRAP] CDPC_ADMIN account already exists in database. Skipping seed.");
        }
    }
}
