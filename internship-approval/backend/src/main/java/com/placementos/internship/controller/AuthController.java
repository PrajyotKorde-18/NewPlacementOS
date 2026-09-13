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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

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
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
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
        private String email;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        private String email;
        private String password;
        private String name;
        private String collegeId;
        private String role;
    }
}
