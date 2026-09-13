package com.placementos.tpms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateDTO {
    private String role; // STUDENT, COORDINATOR, ADMIN
}
