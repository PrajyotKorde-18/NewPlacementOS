package com.placementos.tpms.controller;

import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import com.placementos.tpms.security.JwtUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.placementos.tpms.security.LoginRateLimiterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
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

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole(),
                "id", user.getId()
        ));
    }

    @PostMapping("/register")
    @Transactional
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
                .role("STUDENT")
                .build();

        user = userRepository.save(user);

        // Explicit Student entity creation on registration
        String prefix = user.getEmail().split("@")[0].toUpperCase();
        String collegeId = prefix;
        if (studentRepository.existsByCollegeId(collegeId)) {
            collegeId = prefix + "_" + user.getId();
        }

        studentRepository.save(Student.builder()
                .user(user)
                .collegeId(collegeId)
                .build());

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "token", token,
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole(),
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
    }
}
