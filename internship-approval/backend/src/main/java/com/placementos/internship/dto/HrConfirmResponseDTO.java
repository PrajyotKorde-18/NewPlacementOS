package com.placementos.internship.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrConfirmResponseDTO {
    private boolean success;
    private String status;
    private String message;
}
