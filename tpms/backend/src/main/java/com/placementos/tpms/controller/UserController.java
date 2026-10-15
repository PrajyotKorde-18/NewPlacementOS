package com.placementos.tpms.controller;

import com.placementos.tpms.dto.UserResponseDTO;
import com.placementos.tpms.dto.UserRoleUpdateDTO;
import com.placementos.tpms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/{id}/role")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUserRole(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody UserRoleUpdateDTO dto
    ) {
        return ResponseEntity.ok(userService.updateUserRole(id, dto.getRole()));
    }
}
