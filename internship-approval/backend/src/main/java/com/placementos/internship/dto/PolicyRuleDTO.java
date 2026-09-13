package com.placementos.internship.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRuleDTO {
    private Long id;
    private String ruleName; // System-managed immutable key
    private String label;    // Editable title
    private String ruleType;
    private String ruleValue;
    private Boolean isActive;
    String description;
}
