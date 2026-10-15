package com.placementos.internship.config;

import com.placementos.internship.controller.AuthController;
import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.repository.UserRepository;
import com.placementos.internship.security.LoginRateLimiterService;
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
    private AuthController authController;

    @Autowired
    private LoginRateLimiterService loginRateLimiterService;

    @Test
    @DisplayName("Verify all demo accounts exist with correct roles in internship module")
    void testDemoAccountsExistAndRolesMatch() {
        assertFalse(demoProperties.getAccounts().isEmpty(), "Demo accounts configuration must not be empty");

        for (DemoAccountProperties.DemoAccountConfig config : demoProperties.getAccounts()) {
            Optional<User> userOpt = userRepository.findByEmail(config.getEmail().trim().toLowerCase());
            assertTrue(userOpt.isPresent(), "Demo user must exist in database: " + config.getEmail());

            User user = userOpt.get();
            assertEquals(Role.valueOf(config.getRole().toUpperCase()), user.getRole(),
                    "Role mismatch for demo user: " + config.getEmail());
        }
    }

    @Test
    @DisplayName("Verify all demo accounts authenticate successfully via AuthController")
    void testDemoAccountsCanAuthenticate() {
        for (DemoAccountProperties.DemoAccountConfig config : demoProperties.getAccounts()) {
            AuthController.LoginRequest request = new AuthController.LoginRequest(
                    config.getEmail(),
                    config.getPassword()
            );

            ResponseEntity<?> response = authController.login(request);
            assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "Authentication must succeed for demo account: " + config.getEmail());

            assertNotNull(response.getBody(), "Response body must not be null");
            assertTrue(response.getBody() instanceof Map, "Response body must be a Map");

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertNotNull(body.get("token"), "JWT token must be present in login response");
            assertEquals(config.getEmail().toLowerCase(), body.get("email").toString().toLowerCase(),
                    "Email in response must match demo account email");
            assertEquals(config.getRole().toUpperCase(), body.get("role").toString().toUpperCase(),
                    "Role in response must match expected role");
        }
    }

    @Test
    @DisplayName("Verify demo accounts are exempt from login rate limiting and lockouts")
    void testDemoAccountsExemptFromRateLimiting() {
        for (DemoAccountProperties.DemoAccountConfig config : demoProperties.getAccounts()) {
            String email = config.getEmail();
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
