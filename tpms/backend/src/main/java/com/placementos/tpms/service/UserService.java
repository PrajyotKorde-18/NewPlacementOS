package com.placementos.tpms.service;

import com.placementos.tpms.dto.UserResponseDTO;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.exception.AppException;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponseDTO updateUserRole(Long userId, String newRole) {
        if (newRole == null || newRole.isBlank()) {
            throw new AppException("Role cannot be empty", HttpStatus.BAD_REQUEST);
        }

        String formattedRole = newRole.trim().toUpperCase();
        if (!"STUDENT".equals(formattedRole) && !"COORDINATOR".equals(formattedRole) && !"ADMIN".equals(formattedRole)) {
            throw new AppException("Invalid role: " + newRole + ". Allowed roles: STUDENT, COORDINATOR, ADMIN", HttpStatus.BAD_REQUEST);
        }

        User admin = validateAdminAuthority();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String oldRole = targetUser.getRole();
        targetUser.setRole(formattedRole);
        targetUser = userRepository.save(targetUser);

        log.info("[ADMIN ACTION] User #{} ('{}', {}) role updated from '{}' to '{}' by caller '{}' (ID: {}).",
                targetUser.getId(), targetUser.getName(), targetUser.getEmail(), oldRole, formattedRole, admin.getName(), admin.getId());

        return UserResponseDTO.builder()
                .id(targetUser.getId())
                .email(targetUser.getEmail())
                .name(targetUser.getName())
                .role(targetUser.getRole())
                .build();
    }

    private User validateAdminAuthority() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("Authentication required for role management", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + email));

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new AppException("Access denied: User role updates can only be performed by ADMIN", HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
