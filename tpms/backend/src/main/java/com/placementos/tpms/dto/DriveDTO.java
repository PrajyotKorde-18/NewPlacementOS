package com.placementos.tpms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveDTO {
    private Long id;

    @NotBlank(message = "College ID is required")
    private String collegeId;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Role title is required")
    private String roleTitle;

    @Min(value = 1, message = "Vacancies must be at least 1")
    private Integer vacancies;

    private String ctc;

    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private RoleProfileDTO roleProfile;
}
