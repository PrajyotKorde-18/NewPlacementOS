package com.placementos.internship.service;

import com.placementos.internship.dto.UserResponseDTO;
import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.exception.ResourceNotFoundException;
import com.placementos.internship.repository.UserRepository;
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
    public UserResponseDTO updateUserRole(Long userId, Role newRole) {
        if (newRole == null) {
            throw new AppException("Role cannot be null", HttpStatus.BAD_REQUEST);
        }

        User admin = validateCdpcAdminAuthority();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role oldRole = targetUser.getRole();
        targetUser.setRole(newRole);
        targetUser = userRepository.save(targetUser);

        log.info("[ADMIN ACTION] User #{} ('{}', {}) role updated from '{}' to '{}' by CDPC_ADMIN '{}' (ID: {}).",
                targetUser.getId(), targetUser.getName(), targetUser.getEmail(), oldRole, newRole, admin.getName(), admin.getId());

        return UserResponseDTO.builder()
                .id(targetUser.getId())
                .email(targetUser.getEmail())
                .name(targetUser.getName())
                .role(targetUser.getRole())
                .collegeId(targetUser.getCollegeId())
                .build();
    }

    private User validateCdpcAdminAuthority() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("Authentication required for role management", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated admin not found with email: " + email));

        if (user.getRole() != Role.CDPC_ADMIN) {
            throw new AppException("Access denied: User role updates can only be performed by CDPC_ADMIN", HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
