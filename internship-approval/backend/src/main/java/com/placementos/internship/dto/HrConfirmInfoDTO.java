package com.placementos.internship.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrConfirmInfoDTO {
    private boolean valid;
    private String studentMaskedName;
    private String companyName;
    private String roleTitle;
    private String message;
}
