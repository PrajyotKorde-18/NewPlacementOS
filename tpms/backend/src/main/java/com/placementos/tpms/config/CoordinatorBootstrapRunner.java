package com.placementos.tpms.config;

import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoordinatorBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.coordinator-email:}")
    private String coordinatorEmail;

    @Value("${app.bootstrap.coordinator-password:}")
    private String coordinatorPassword;

    @Value("${app.bootstrap.coordinator-name:System Placement Coordinator}")
    private String coordinatorName;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole("COORDINATOR") && !userRepository.existsByRole("ADMIN")) {
            if (coordinatorEmail == null || coordinatorEmail.isBlank() || coordinatorPassword == null || coordinatorPassword.isBlank()) {
                log.warn("[BOOTSTRAP WARNING] No COORDINATOR or ADMIN found in database, but environment variables BOOTSTRAP_COORDINATOR_EMAIL and/or BOOTSTRAP_COORDINATOR_PASSWORD are not provided. Skipping coordinator seed.");
                return;
            }

            log.info("[BOOTSTRAP] No COORDINATOR found in system. Seeding initial bootstrap Placement Coordinator account from environment configuration...");
            User coordinator = User.builder()
                    .email(coordinatorEmail.trim())
                    .password(passwordEncoder.encode(coordinatorPassword.trim()))
                    .name(coordinatorName.trim())
                    .role("COORDINATOR")
                    .build();

            userRepository.save(coordinator);
            log.info("[BOOTSTRAP] Initial COORDINATOR account created successfully with email: '{}'.", coordinatorEmail.trim());
        } else {
            log.info("[BOOTSTRAP] COORDINATOR/ADMIN account already exists in database. Skipping seed.");
        }
    }
}
