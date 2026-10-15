package com.placementos.tpms.dto;

import com.placementos.tpms.entity.PlacementStatus;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFullProfileDTO {

    private Long studentId;
    private String collegeId;
    private String collegeRollNumber;
    private String phoneNumber;
    private PlacementStatus placementStatus;
    private String email;

    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String summary;
    private String achievements;

    private AcademicProfileDTO academicProfile;

    @Builder.Default
    private List<CertificationDTO> certifications = new ArrayList<>();

    // Dynamic resume fields
    private Object skills;
    private Object projects;
    private Object internships;
    private Object codingProfiles;
    private String extractionSource;
}
