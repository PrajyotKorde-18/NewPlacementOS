package com.placementos.tpms.config;

import com.placementos.tpms.controller.AuthController;
import com.placementos.tpms.entity.AcademicProfile;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.AcademicProfileRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import com.placementos.tpms.security.LoginRateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@Transactional
public class DemoAccountPreservationTest {

    @Autowired
    private DemoAccountProperties demoProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademicProfileRepository academicProfileRepository;

    @Autowired
    private AuthController authController;

    @Autowired
    private LoginRateLimiterService loginRateLimiterService;

    @Test
    @DisplayName("Verify all demo accounts are seeded and have correct roles")
    void testDemoAccountsExistAndRolesMatch() {
        assertFalse(demoProperties.getAccounts().isEmpty(), "Demo accounts list must not be empty in demo profile");

        for (DemoAccountProperties.DemoAccountConfig accountConfig : demoProperties.getAccounts()) {
            Optional<User> userOpt = userRepository.findByEmail(accountConfig.getEmail().trim().toLowerCase());
            assertTrue(userOpt.isPresent(), "Demo user must exist in database: " + accountConfig.getEmail());

            User user = userOpt.get();
            assertEquals(accountConfig.getRole().toUpperCase(), user.getRole().toUpperCase(),
                    "Role mismatch for demo user: " + accountConfig.getEmail());
        }
    }

    @Test
    @DisplayName("Verify each demo account can successfully authenticate via AuthController")
    void testDemoAccountsCanAuthenticate() {
        for (DemoAccountProperties.DemoAccountConfig accountConfig : demoProperties.getAccounts()) {
            AuthController.LoginRequest request = new AuthController.LoginRequest(
                    accountConfig.getEmail(),
                    accountConfig.getPassword()
            );

            ResponseEntity<?> response = authController.login(request);
            assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "Authentication must succeed for demo account: " + accountConfig.getEmail());

            assertNotNull(response.getBody(), "Response body must not be null");
            assertTrue(response.getBody() instanceof Map, "Response body must be a Map");

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertNotNull(body.get("token"), "JWT token must be present in login response");
            assertEquals(accountConfig.getEmail().toLowerCase(), body.get("email").toString().toLowerCase(),
                    "Email in response must match demo account email");
            assertEquals(accountConfig.getRole().toUpperCase(), body.get("role").toString().toUpperCase(),
                    "Role in response must match expected role");
        }
    }

    @Test
    @DisplayName("Verify student demo accounts are seeded with verified academic profiles")
    void testDemoStudentsHaveVerifiedAcademicProfiles() {
        for (DemoAccountProperties.DemoAccountConfig accountConfig : demoProperties.getAccounts()) {
            if ("STUDENT".equalsIgnoreCase(accountConfig.getRole())) {
                User user = userRepository.findByEmail(accountConfig.getEmail().toLowerCase()).orElseThrow();
                Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                assertNotNull(student, "Student record must exist for demo student: " + accountConfig.getEmail());

                AcademicProfile profile = academicProfileRepository.findByStudentId(student.getId()).orElse(null);
                assertNotNull(profile, "Academic profile must exist for demo student: " + accountConfig.getEmail());
                assertNotNull(profile.getCurrentCgpa(), "Current CGPA must be calculated and non-null");
                assertTrue(profile.getCurrentCgpa() >= 7.5, "Demo student CGPA must be >= 7.5 (verified status)");
                assertEquals(0, profile.getActiveBacklogCount(), "Demo student must have 0 active backlogs");
            }
        }
    }

    @Test
    @DisplayName("Verify demo accounts are exempt from login rate limiting and lockouts")
    void testDemoAccountsExemptFromRateLimiting() {
        for (DemoAccountProperties.DemoAccountConfig accountConfig : demoProperties.getAccounts()) {
            String email = accountConfig.getEmail();
            assertTrue(loginRateLimiterService.isDemoExempt(email),
                    "Demo account must be marked exempt from rate limiting: " + email);

            // Attempt 10 failed recordings
            for (int i = 0; i < 10; i++) {
                loginRateLimiterService.recordFailedAttempt(email);
            }

            // Verify checkLoginAllowed still passes without throwing TOO_MANY_REQUESTS
            assertDoesNotThrow(() -> loginRateLimiterService.checkLoginAllowed(email),
                    "checkLoginAllowed must never throw for exempt demo account: " + email);
        }
    }
}
