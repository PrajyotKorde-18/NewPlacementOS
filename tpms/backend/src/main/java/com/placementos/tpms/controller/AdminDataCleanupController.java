package com.placementos.tpms.controller;

import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/data")
@Slf4j
public class AdminDataCleanupController {

    private final ApplicationRepository applicationRepository;
    private final DriveRepository driveRepository;
    private final AcademicProfileRepository academicProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final java.util.Optional<com.placementos.tpms.config.DemoAccountSeeder> demoAccountSeeder;

    @PersistenceContext
    private final EntityManager entityManager;

    public AdminDataCleanupController(
            ApplicationRepository applicationRepository,
            DriveRepository driveRepository,
            AcademicProfileRepository academicProfileRepository,
            StudentProfileRepository studentProfileRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            ResumeVersionRepository resumeVersionRepository,
            java.util.Optional<com.placementos.tpms.config.DemoAccountSeeder> demoAccountSeeder,
            EntityManager entityManager
    ) {
        this.applicationRepository = applicationRepository;
        this.driveRepository = driveRepository;
        this.academicProfileRepository = academicProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.demoAccountSeeder = demoAccountSeeder;
        this.entityManager = entityManager;
    }

    @PostMapping("/cleanup")
    @Transactional
    public ResponseEntity<?> cleanupAllData() {
        log.warn("[ADMIN CLEANUP] Purging all student entries and placement company drives from database...");

        long appCount = applicationRepository.count();
        long driveCount = driveRepository.count();
        long resumeCount = resumeVersionRepository.count();
        long academicCount = academicProfileRepository.count();
        long profileCount = studentProfileRepository.count();
        long studentCount = studentRepository.count();
        List<User> studentUsers = userRepository.findByRole("STUDENT");
        int userCount = studentUsers.size();

        // 1. Disable FK checks temporarily for bulk purge safety
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        // 2. Delete child tables in reverse dependency order
        entityManager.createNativeQuery("DELETE FROM applications").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM role_profiles").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM drives").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM semester_sgpas").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM backlog_records").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM academic_profiles").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM student_certifications").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM student_profiles").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM resume_versions").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM students").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users WHERE role = 'STUDENT'").executeUpdate();

        // 3. Re-enable FK checks
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        // 4. If running in demo profile, restore pristine verified demo student accounts
        demoAccountSeeder.ifPresent(seeder -> {
            log.info("[ADMIN CLEANUP] Re-seeding verified demo accounts post-cleanup...");
            seeder.seedDemoAccounts();
        });

        log.info("[ADMIN CLEANUP COMPLETED] Purged {} applications, {} drives, {} resumes, {} academic profiles, {} profiles, {} student records, and {} student user accounts.",
                appCount, driveCount, resumeCount, academicCount, profileCount, studentCount, userCount);

        return ResponseEntity.ok(Map.of(
                "message", "Successfully purged all student entries and company placement drives from database.",
                "deletedApplications", appCount,
                "deletedCompanyDrives", driveCount,
                "deletedStudentProfiles", profileCount,
                "deletedStudentRecords", studentCount,
                "deletedStudentUserAccounts", userCount
        ));
    }
}
