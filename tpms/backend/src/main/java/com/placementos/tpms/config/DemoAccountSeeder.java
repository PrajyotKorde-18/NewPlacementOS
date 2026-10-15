package com.placementos.tpms.config;

import com.placementos.tpms.entity.*;
import com.placementos.tpms.repository.AcademicProfileRepository;
import com.placementos.tpms.repository.StudentProfileRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
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
    private final AcademicProfileRepository academicProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DemoAccountSeeder(
            DemoAccountProperties demoProperties,
            UserRepository userRepository,
            StudentRepository studentRepository,
            AcademicProfileRepository academicProfileRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager
    ) {
        this.demoProperties = demoProperties;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.academicProfileRepository = academicProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
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

        log.info("[DEMO SEEDER] Checking and seeding required demo accounts under 'demo' profile...");

        for (DemoAccountProperties.DemoAccountConfig config : demoProperties.getAccounts()) {
            if (config.getEmail() == null || config.getEmail().isBlank()) {
                continue;
            }

            String email = config.getEmail().trim().toLowerCase();
            Optional<User> existingUser = userRepository.findByEmail(email);

            User user;
            if (existingUser.isEmpty()) {
                user = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(config.getPassword() != null ? config.getPassword().trim() : "Demo@12345"))
                        .name(config.getName() != null ? config.getName().trim() : "Demo User")
                        .role(config.getRole() != null ? config.getRole().trim().toUpperCase() : "STUDENT")
                        .build();
                user = userRepository.saveAndFlush(user);
                log.info("[DEMO SEEDER] Seeded missing demo user: {} with role: {}", email, user.getRole());
            } else {
                user = existingUser.get();
                log.info("[DEMO SEEDER] Demo user already exists: {} (Role: {}). Preserving existing credentials.", email, user.getRole());
            }

            // If the account has STUDENT role, ensure Student record and AcademicProfile are fully verified
            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                seedStudentProfiles(user, config);
            }
        }

        log.info("[DEMO SEEDER] Demo accounts verification and seeding completed successfully.");
    }

    private void seedStudentProfiles(User user, DemoAccountProperties.DemoAccountConfig config) {
        User managedUser = entityManager.find(User.class, user.getId());
        Student student = studentRepository.findByUserId(managedUser.getId()).orElse(null);

        if (student == null) {
            String collegeId = config.getCollegeId() != null && !config.getCollegeId().isBlank()
                    ? config.getCollegeId().trim()
                    : ("STU_" + managedUser.getId());

            if (studentRepository.existsByCollegeId(collegeId)) {
                collegeId = collegeId + "_" + managedUser.getId();
            }

            String rollNo = config.getCollegeRollNumber() != null && !config.getCollegeRollNumber().isBlank()
                    ? config.getCollegeRollNumber().trim()
                    : "2022CS099";

            if (studentRepository.existsByCollegeRollNumber(rollNo)) {
                rollNo = rollNo + "_" + managedUser.getId();
            }

            student = Student.builder()
                    .user(managedUser)
                    .collegeId(collegeId)
                    .collegeRollNumber(rollNo)
                    .phoneNumber(config.getPhoneNumber() != null ? config.getPhoneNumber().trim() : "9876543210")
                    .placementStatus(PlacementStatus.NOT_PLACED)
                    .build();
            student = studentRepository.saveAndFlush(student);
            log.info("[DEMO SEEDER] Created student record for demo user: {} (Roll: {})", managedUser.getEmail(), student.getCollegeRollNumber());
        }

        Student managedStudent = entityManager.find(Student.class, student.getId());

        // Academic profile verification
        Optional<AcademicProfile> existingAcademicProfile = academicProfileRepository.findByStudentId(managedStudent.getId());
        if (existingAcademicProfile.isEmpty()) {
            Double cgpa = config.getCgpa() != null ? config.getCgpa() : 8.5;
            AcademicProfile profile = AcademicProfile.builder()
                    .student(managedStudent)
                    .studentId(managedStudent.getId())
                    .tenthPercentage(config.getTenthPercentage() != null ? config.getTenthPercentage() : 85.0)
                    .tenthCgpa(8.5)
                    .tenthBoard("CBSE")
                    .tenthYearOfPassing(2020)
                    .twelfthPercentage(config.getTwelfthPercentage() != null ? config.getTwelfthPercentage() : 82.0)
                    .twelfthCgpa(8.2)
                    .twelfthBoard("CBSE")
                    .twelfthStream("Science (PCM)")
                    .twelfthYearOfPassing(2022)
                    .branch(config.getBranch() != null ? config.getBranch() : "Computer Science & Engineering")
                    .admissionYear(2022)
                    .expectedGraduationYear(2026)
                    .build();

            // Seed two finalized semesters for verified CGPA calculation
            SemesterSgpa sem1 = SemesterSgpa.builder()
                    .academicProfile(profile)
                    .semesterNumber(1)
                    .sgpa(cgpa)
                    .isFinalized(true)
                    .build();
            SemesterSgpa sem2 = SemesterSgpa.builder()
                    .academicProfile(profile)
                    .semesterNumber(2)
                    .sgpa(cgpa)
                    .isFinalized(true)
                    .build();

            profile.getSemesterSgpas().add(sem1);
            profile.getSemesterSgpas().add(sem2);

            entityManager.persist(profile);
            entityManager.flush();
            log.info("[DEMO SEEDER] Seeded verified academic profile for demo student: {} (CGPA: {}, Branch: {})",
                    managedUser.getEmail(), cgpa, profile.getBranch());
        }

        // Student profile & skills
        Optional<StudentProfile> existingStudentProfile = studentProfileRepository.findByStudentId(managedStudent.getId());
        if (existingStudentProfile.isEmpty()) {
            String skills = config.getSkills() != null && !config.getSkills().isBlank()
                    ? config.getSkills()
                    : "Java, Spring Boot, React, MySQL, JavaScript";

            StudentProfile studentProfile = StudentProfile.builder()
                    .student(managedStudent)
                    .studentId(managedStudent.getId())
                    .skills(skills)
                    .summary("Pre-verified demo student candidate for PlacementOS recruitment drives and evaluations.")
                    .achievements("Dean's Merit List, Smart India Hackathon Finalist")
                    .build();

            entityManager.persist(studentProfile);
            entityManager.flush();
            log.info("[DEMO SEEDER] Seeded verified student profile and skills for demo student: {}", managedUser.getEmail());
        }
    }
}
