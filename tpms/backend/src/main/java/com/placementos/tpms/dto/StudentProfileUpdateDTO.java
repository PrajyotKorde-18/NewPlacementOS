package com.placementos.tpms.dto;

import com.placementos.tpms.entity.PlacementStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileUpdateDTO {

    private String collegeRollNumber;

    @Pattern(
        regexp = "^$|^(\\+?[0-9]{1,3})?[0-9\\-\\s]{10,15}$",
        message = "Phone number must be a valid phone number (10 to 15 digits)"
    )
    private String phoneNumber;

    private PlacementStatus placementStatus;

    @Pattern(
        regexp = "^$|^(https?://)?([a-zA-Z0-9.-]+\\.)?linkedin\\.com(/.*)?$",
        message = "LinkedIn URL must be a valid LinkedIn URL"
    )
    private String linkedinUrl;

    @Pattern(
        regexp = "^$|^(https?://)?([a-zA-Z0-9.-]+\\.)?github\\.com(/.*)?$",
        message = "GitHub URL must be a valid GitHub URL"
    )
    private String githubUrl;

    @Pattern(
        regexp = "^$|^(https?://)?([a-zA-Z0-9.-]+)(:[0-9]+)?(/.*)?$",
        message = "Portfolio URL must be a valid URL"
    )
    private String portfolioUrl;

    @Valid
    private AcademicProfileDTO academicProfile;

    @Valid
    private List<CertificationDTO> structuredCertifications; // Default null to distinguish omitted vs []

    // Optional legacy JSON fields preserved
    private String summary;
    private String achievements;
}
