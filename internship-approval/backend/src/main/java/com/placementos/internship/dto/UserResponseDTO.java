package com.placementos.internship.dto;

import com.placementos.internship.entity.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String email;
    private String name;
    private Role role;
    private String collegeId;
}
