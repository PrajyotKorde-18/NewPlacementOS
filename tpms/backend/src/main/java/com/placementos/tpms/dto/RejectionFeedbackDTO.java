package com.placementos.tpms.dto;

import com.placementos.tpms.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectionFeedbackDTO {
    private Long applicationId;
    private Long driveId;
    private String companyName;
    private String roleTitle;

    private Long studentId;
    private String studentName;
    private ApplicationStatus status;
    private Integer rankPosition;
    private Integer totalVacancies;
    private Double totalScore;

    private String feedbackSummary;
    private List<String> missingMustHaveSkills;
    private String relativelyWeakestCategory;
    private List<String> weakCategories;
    private String coordinatorOverrideReason;
}
