package com.placementos.internship.dto;

import com.placementos.internship.entity.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleUpdateDTO {
    @jakarta.validation.constraints.NotNull(message = "Role is required")
    private Role role;
}
