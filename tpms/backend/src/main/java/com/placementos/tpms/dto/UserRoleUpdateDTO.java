package com.placementos.tpms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateDTO {
    @jakarta.validation.constraints.NotBlank(message = "Role is required")
    @jakarta.validation.constraints.Pattern(regexp = "^(STUDENT|COORDINATOR|ADMIN)$", message = "Role must be STUDENT, COORDINATOR, or ADMIN")
    private String role; // STUDENT, COORDINATOR, ADMIN
}
