package com.placementos.internship.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEvaluationResultDTO {
    private String ruleName;
    private boolean passed;
    private String failureReason;
}
