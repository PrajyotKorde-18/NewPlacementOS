package com.placementos.internship.dto;

import com.placementos.internship.entity.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleUpdateDTO {
    private Role role;
}
