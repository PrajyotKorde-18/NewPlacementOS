package com.placementos.internship.controller;

import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.repository.UserRepository;
import com.placementos.internship.security.JwtUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.placementos.internship.security.LoginRateLimiterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final LoginRateLimiterService loginRateLimiterService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = loginRateLimiterService.getClientIp(httpRequest);
        return doLogin(request, clientIp);
    }

    public ResponseEntity<?> login(LoginRequest request) {
        return doLogin(request, "unknown");
    }

    private ResponseEntity<?> doLogin(LoginRequest request, String clientIp) {
        loginRateLimiterService.checkLoginAllowed(request.getEmail(), clientIp);

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiterService.recordFailedAttempt(request.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        loginRateLimiterService.recordSuccess(request.getEmail(), clientIp);

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "id", user.getId()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (!jwtUtils.isAllowedDomain(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email domain not allowed. Must be @rknec.edu"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = User.builder()
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .name(request.getName().trim())
                .collegeId(request.getCollegeId() != null ? request.getCollegeId().trim() : "STU001")
                .role(request.getRole() != null ? Role.valueOf(request.getRole()) : Role.STUDENT)
                .build();

        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "token", token,
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "id", user.getId()
        ));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        private String collegeId;
        private String role;
    }
}
