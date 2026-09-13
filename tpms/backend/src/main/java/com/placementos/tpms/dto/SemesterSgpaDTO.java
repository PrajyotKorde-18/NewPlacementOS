package com.placementos.tpms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterSgpaDTO {

    private Long id;

    @NotNull(message = "Semester number is required")
    @Min(value = 1, message = "Semester number must be at least 1")
    @Max(value = 10, message = "Semester number cannot exceed 10")
    private Integer semesterNumber;

    @NotNull(message = "SGPA is required")
    @DecimalMin(value = "0.0", message = "SGPA cannot be negative")
    @DecimalMax(value = "10.0", message = "SGPA cannot exceed 10.0")
    private Double sgpa;

    @NotNull(message = "isFinalized flag is required")
    @Builder.Default
    private Boolean isFinalized = true;
}
