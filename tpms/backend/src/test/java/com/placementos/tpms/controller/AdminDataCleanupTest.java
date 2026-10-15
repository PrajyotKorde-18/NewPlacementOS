package com.placementos.tpms.controller;

import com.placementos.tpms.entity.Drive;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.DriveRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@Transactional
public class AdminDataCleanupTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminDataCleanupController adminDataCleanupController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DriveRepository driveRepository;

    @Test
    @DisplayName("Admin Cleanup: Disabled by default returns HTTP 403 Forbidden")
    @WithMockUser(username = "admin@rknec.edu", roles = {"ADMIN"})
    void testCleanup_DisabledByDefault_ReturnsForbidden() throws Exception {
        boolean original = adminDataCleanupController.isCleanupEnabled();
        try {
            adminDataCleanupController.setCleanupEnabled(false);
            mockMvc.perform(post("/admin/data/cleanup"))
                    .andExpect(status().isForbidden());
        } finally {
            adminDataCleanupController.setCleanupEnabled(original);
        }
    }

    @Test
    @DisplayName("Admin Cleanup: Forbidden for COORDINATOR role")
    @WithMockUser(username = "coord_user@rknec.edu", roles = {"COORDINATOR"})
    void testCleanup_ForbiddenForCoordinator() throws Exception {
        mockMvc.perform(post("/admin/data/cleanup"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Cleanup: Forbidden for unauthenticated users")
    void testCleanup_ForbiddenForLoggedOut() throws Exception {
        mockMvc.perform(post("/admin/data/cleanup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin Cleanup: Allowed for ADMIN when enabled; demo accounts remain available")
    @WithMockUser(username = "admin@rknec.edu", roles = {"ADMIN"})
    void testCleanup_AllowedForAdminWhenEnabled_DemoAccountsPreserved() throws Exception {
        boolean original = adminDataCleanupController.isCleanupEnabled();
        try {
            adminDataCleanupController.setCleanupEnabled(true);

            // Seed a transient non-demo student & company drive to verify purge
            User transientStudent = userRepository.save(User.builder()
                    .email("transient.student@rknec.edu")
                    .name("Transient Student")
                    .password("pass")
                    .role("STUDENT")
                    .build());

            studentRepository.save(Student.builder()
                    .user(transientStudent)
                    .collegeId("TRANS001")
                    .collegeRollNumber("TROLL001")
                    .build());

            User adminUser = userRepository.findByEmail("admin@rknec.edu").orElseThrow();
            driveRepository.save(Drive.builder()
                    .companyName("Transient Corp")
                    .roleTitle("Intern")
                    .collegeId("RKNECO1")
                    .createdBy(adminUser)
                    .build());

            // Perform cleanup via MockMvc
            mockMvc.perform(post("/admin/data/cleanup"))
                    .andExpect(status().isOk());

            // Transient data purged
            assertFalse(userRepository.findByEmail("transient.student@rknec.edu").isPresent(),
                    "Transient student user must be purged");
            assertEquals(0, driveRepository.count(), "All drives must be purged");

            // Demo accounts remain available per the preservation contract
            assertTrue(userRepository.findByEmail("candidate.student@rknec.edu").isPresent(),
                    "Demo student 'candidate.student@rknec.edu' must remain available after cleanup");
            assertTrue(userRepository.findByEmail("student1@rknec.edu").isPresent(),
                    "Demo student 'student1@rknec.edu' must remain available after cleanup");
            assertTrue(userRepository.findByEmail("admin@rknec.edu").isPresent(),
                    "Demo admin 'admin@rknec.edu' must remain available after cleanup");

            // Verify demo student record and academic profile restored
            User demoStudentUser = userRepository.findByEmail("candidate.student@rknec.edu").orElseThrow();
            assertTrue(studentRepository.findByUserId(demoStudentUser.getId()).isPresent(),
                    "Demo student entity must be present and verified");
        } finally {
            adminDataCleanupController.setCleanupEnabled(original);
        }
    }
}
