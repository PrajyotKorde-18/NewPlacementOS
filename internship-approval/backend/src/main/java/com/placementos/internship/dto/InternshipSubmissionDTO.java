package com.placementos.internship.dto;

import com.placementos.internship.entity.InternshipMode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipSubmissionDTO {
    private Long studentId;
    private String companyName;
    private String roleTitle;
    private Integer durationWeeks;
    private BigDecimal stipend;
    private LocalDate startDate;
    private LocalDate endDate;
    private InternshipMode mode;
    private String hrContactEmail;
}
