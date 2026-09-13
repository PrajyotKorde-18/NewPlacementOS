package com.placementos.internship.service;

import com.placementos.internship.dto.UserResponseDTO;
import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private User adminUser;
    private User staffUser;
    private User targetStudent;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);

        adminUser = User.builder()
                .id(1L)
                .name("Super Admin")
                .email("admin@rknec.edu")
                .role(Role.CDPC_ADMIN)
                .collegeId("ADMIN001")
                .build();

        staffUser = User.builder()
                .id(2L)
                .name("Staff Member")
                .email("staff@rknec.edu")
                .role(Role.CDPC_STAFF)
                .collegeId("STAFF002")
                .build();

        targetStudent = User.builder()
                .id(3L)
                .name("John Doe")
                .email("john@rknec.edu")
                .role(Role.STUDENT)
                .collegeId("STU003")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateUserRole_CdpcAdminCanPromoteStudentToCdpcStaff() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminUser.getEmail(), null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("admin@rknec.edu")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetStudent));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponseDTO response = userService.updateUserRole(3L, Role.CDPC_STAFF);

        assertNotNull(response);
        assertEquals(Role.CDPC_STAFF, response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateUserRole_CdpcStaffCannotPromote_AccessDenied() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staffUser.getEmail(), null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("staff@rknec.edu")).thenReturn(Optional.of(staffUser));

        AppException ex = assertThrows(AppException.class, () ->
                userService.updateUserRole(3L, Role.CDPC_ADMIN)
        );
        assertTrue(ex.getMessage().contains("Access denied"));
    }
}
