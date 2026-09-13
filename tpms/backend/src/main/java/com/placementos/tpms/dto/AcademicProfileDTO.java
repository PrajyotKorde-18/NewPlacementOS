package com.placementos.tpms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicProfileDTO {

    @DecimalMin(value = "0.0", message = "10th percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "10th percentage cannot exceed 100.0")
    private Double tenthPercentage;

    @DecimalMin(value = "0.0", message = "10th CGPA cannot be negative")
    @DecimalMax(value = "10.0", message = "10th CGPA cannot exceed 10.0")
    private Double tenthCgpa;

    private String tenthBoard;

    @Min(value = 1990, message = "Invalid 10th passing year")
    @Max(value = 2100, message = "Invalid 10th passing year")
    private Integer tenthYearOfPassing;

    @DecimalMin(value = "0.0", message = "12th percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "12th percentage cannot exceed 100.0")
    private Double twelfthPercentage;

    @DecimalMin(value = "0.0", message = "12th CGPA cannot be negative")
    @DecimalMax(value = "10.0", message = "12th CGPA cannot exceed 10.0")
    private Double twelfthCgpa;

    private String twelfthBoard;
    private String twelfthStream;

    @Min(value = 1990, message = "Invalid 12th passing year")
    @Max(value = 2100, message = "Invalid 12th passing year")
    private Integer twelfthYearOfPassing;

    private String branch;

    @Min(value = 1990, message = "Invalid admission year")
    @Max(value = 2100, message = "Invalid admission year")
    private Integer admissionYear;

    @Min(value = 1990, message = "Invalid expected graduation year")
    @Max(value = 2100, message = "Invalid expected graduation year")
    private Integer expectedGraduationYear;

    // Derived response fields
    private Double currentCgpa;
    private Long activeBacklogCount;
    private Long totalEverBacklogCount;

    @Valid
    private List<SemesterSgpaDTO> semesterSgpas; // Default null to support partial updates

    @Valid
    private List<BacklogRecordDTO> backlogRecords; // Default null to support partial updates
}
