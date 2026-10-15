package com.placementos.internship.security;

import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LoginRateLimiterTest {

    @Autowired
    private LoginRateLimiterService rateLimiterService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        rateLimiterService.clearAll();
    }

    @Test
    @DisplayName("Unit: One IP locking out does not lock out another IP for the same email")
    void testUnit_OneIpCannotLockOutAnotherIp() {
        String email = "target.user@rknec.edu";
        String ip1 = "192.168.1.100";
        String ip2 = "192.168.1.200";

        // Record 5 failed attempts from ip1
        for (int i = 0; i < 5; i++) {
            rateLimiterService.recordFailedAttempt(email, ip1);
        }

        // ip1 must be locked out
        AppException ex = assertThrows(AppException.class, () ->
                rateLimiterService.checkLoginAllowed(email, ip1)
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());

        // ip2 must NOT be locked out for the same email
        assertDoesNotThrow(() -> rateLimiterService.checkLoginAllowed(email, ip2));
    }

    @Test
    @DisplayName("Unit: Demo accounts are exempt from lockout regardless of attempts or IP")
    void testUnit_DemoAccountsExemptFromLockout() {
        String demoEmail = "student1@rknec.edu";
        String ip = "10.0.0.5";

        assertTrue(rateLimiterService.isDemoExempt(demoEmail));

        for (int i = 0; i < 10; i++) {
            rateLimiterService.recordFailedAttempt(demoEmail, ip);
        }

        assertDoesNotThrow(() -> rateLimiterService.checkLoginAllowed(demoEmail, ip));
    }

    @Test
    @DisplayName("MockMvc: Real login lockout isolated by client IP via X-Forwarded-For header")
    void testMockMvc_LoginLockoutIsolatedByClientIp() throws Exception {
        userRepository.save(User.builder()
                .name("Normal User")
                .email("lockout.test@rknec.edu")
                .password(passwordEncoder.encode("correct_pass123"))
                .role(Role.STUDENT)
                .collegeId("COLL_NORM")
                .build());

        String attackerIp = "203.0.113.195";
        String legitimateIp = "198.51.100.42";
        String badPayload = "{\"email\":\"lockout.test@rknec.edu\",\"password\":\"wrong_password\"}";

        // Attacker performs 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", attackerIp)
                            .content(badPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt from attacker IP should be locked out (429 TOO_MANY_REQUESTS)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", attackerIp)
                        .content(badPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Account temporarily locked")));

        // Legitimate user from a different IP is NOT locked out (receives 401 unauthorized for bad password, not 429)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", legitimateIp)
                        .content(badPayload))
                .andExpect(status().isUnauthorized());

        // Legitimate user with correct password succeeds from legitimate IP
        String goodPayload = "{\"email\":\"lockout.test@rknec.edu\",\"password\":\"correct_pass123\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", legitimateIp)
                        .content(goodPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("MockMvc: Demo accounts cannot be locked out via login endpoint")
    void testMockMvc_DemoAccountsExemptFromLockout() throws Exception {
        userRepository.findByEmail("student1@rknec.edu")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Demo Student")
                        .email("student1@rknec.edu")
                        .password(passwordEncoder.encode("secret_pass"))
                        .role(Role.STUDENT)
                        .collegeId("COLL_DEMO")
                        .build()));

        String ip = "192.168.5.5";
        String badPayload = "{\"email\":\"student1@rknec.edu\",\"password\":\"wrong_pass\"}";

        // 7 failed attempts
        for (int i = 0; i < 7; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", ip)
                            .content(badPayload))
                    .andExpect(status().isUnauthorized()); // Still 401, never 429
        }
    }
}
