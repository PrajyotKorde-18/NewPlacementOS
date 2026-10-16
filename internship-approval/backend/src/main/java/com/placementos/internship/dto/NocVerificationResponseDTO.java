package com.placementos.internship.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NocVerificationResponseDTO {
    private boolean valid;
    private String studentName; // masked e.g. "A*** J***"
    private String companyName;
    private String roleTitle;
    private String approvalDate; // formatted approval date only
    private String message;
}
