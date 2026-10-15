package com.placementos.internship.config;

import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.Student;
import com.placementos.internship.entity.User;
import com.placementos.internship.repository.StudentRepository;
import com.placementos.internship.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("demo")
@Order(20)
@Transactional
public class DemoAccountSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountSeeder.class);

    private final DemoAccountProperties demoProperties;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DemoAccountSeeder(
            DemoAccountProperties demoProperties,
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager
    ) {
        this.demoProperties = demoProperties;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDemoAccounts();
    }

    @Transactional
    public void seedDemoAccounts() {
        if (!demoProperties.isEnabled()) {
            log.info("[DEMO SEEDER] Demo account seeding is disabled via configuration.");
            return;
        }

        log.info("[DEMO SEEDER] Checking and seeding required demo accounts in internship-approval under 'demo' profile...");

        for (DemoAccountProperties.DemoAccountConfig config : demoProperties.getAccounts()) {
            if (config.getEmail() == null || config.getEmail().isBlank()) {
                continue;
            }

            String email = config.getEmail().trim().toLowerCase();
            Optional<User> existingUser = userRepository.findByEmail(email);

            User targetUser;
            if (existingUser.isEmpty()) {
                Role role = Role.valueOf(config.getRole() != null ? config.getRole().trim().toUpperCase() : "STUDENT");
                String collegeId = config.getCollegeId() != null && !config.getCollegeId().isBlank()
                        ? config.getCollegeId().trim()
                        : "STU001";

                User user = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(config.getPassword() != null ? config.getPassword().trim() : "Demo@12345"))
                        .name(config.getName() != null ? config.getName().trim() : "Demo User")
                        .role(role)
                        .collegeId(collegeId)
                        .build();

                targetUser = userRepository.saveAndFlush(user);
                log.info("[DEMO SEEDER] Seeded missing demo user: {} (Role: {}, CollegeID: {})", email, targetUser.getRole(), targetUser.getCollegeId());
            } else {
                targetUser = existingUser.get();
                log.info("[DEMO SEEDER] Demo user already exists: {} (Role: {}). Preserving existing credentials.", email, targetUser.getRole());
            }

            if (targetUser.getRole() == Role.STUDENT && studentRepository.findByUserId(targetUser.getId()).isEmpty()) {
                Student student = Student.builder()
                        .user(targetUser)
                        .department(config.getBranch() != null ? config.getBranch() : "Computer Science")
                        .gpa(config.getCgpa() != null ? config.getCgpa() : 8.5)
                        .build();
                studentRepository.saveAndFlush(student);
                log.info("[DEMO SEEDER] Seeded verified student record for {}: GPA={}, Dept={}", email, student.getGpa(), student.getDepartment());
            }
        }

        log.info("[DEMO SEEDER] Demo accounts verification and seeding completed successfully.");
    }
}
