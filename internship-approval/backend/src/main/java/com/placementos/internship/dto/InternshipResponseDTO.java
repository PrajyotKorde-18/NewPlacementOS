package com.placementos.internship.dto;

import com.placementos.internship.entity.InternshipMode;
import com.placementos.internship.entity.InternshipRequestStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String collegeId;
    private String companyName;
    private String roleTitle;
    private Integer durationWeeks;
    private BigDecimal stipend;
    private LocalDate startDate;
    private LocalDate endDate;
    private InternshipMode mode;
    private String hrContactEmail;
    private String offerLetterUrl;
    private InternshipRequestStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private List<RuleEvaluationResultDTO> ruleEvaluations;
    private List<ScamFlagDTO> scamFlags;
    private String hrConfirmationStatus;
    private Integer hrConfirmationResendCount;
    private LocalDateTime hrConfirmationSentAt;
    private LocalDateTime hrConfirmationRespondedAt;
}
