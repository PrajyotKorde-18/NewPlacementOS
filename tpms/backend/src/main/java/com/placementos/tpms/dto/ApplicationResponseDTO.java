package com.placementos.tpms.dto;

import com.placementos.tpms.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDTO {
    private Long id;
    private Long driveId;
    private String companyName;
    private String roleTitle;

    private Long studentId;
    private String collegeId;
    private String studentName;
    private String studentEmail;

    private Double score;
    private Integer rankPosition;
    private ApplicationStatus status;
    private ScoreBreakdownDTO scoreBreakdown;

    private String overrideReason;
    private Long overriddenByUserId;
    private String overriddenByUserName;

    private LocalDateTime appliedAt;
}
