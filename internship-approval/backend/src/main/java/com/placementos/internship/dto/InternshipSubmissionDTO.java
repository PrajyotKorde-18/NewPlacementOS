package com.placementos.internship.dto;

import com.placementos.internship.entity.InternshipMode;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipSubmissionDTO {

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotBlank(message = "companyName is required")
    @Size(max = 150, message = "companyName cannot exceed 150 characters")
    private String companyName;

    @NotBlank(message = "roleTitle is required")
    @Size(max = 150, message = "roleTitle cannot exceed 150 characters")
    private String roleTitle;

    @NotNull(message = "durationWeeks is required")
    @Min(value = 1, message = "Duration must be at least 1 week")
    @Max(value = 104, message = "Duration cannot exceed 104 weeks")
    private Integer durationWeeks;

    @NotNull(message = "stipend is required")
    @DecimalMin(value = "0.0", message = "Stipend cannot be negative")
    private BigDecimal stipend;

    private LocalDate startDate;
    private LocalDate endDate;

    private InternshipMode mode;

    @NotBlank(message = "hrContactEmail is required")
    @Email(message = "Invalid HR contact email format")
    private String hrContactEmail;
}
