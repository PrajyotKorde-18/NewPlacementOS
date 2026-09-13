package com.placementos.internship.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScamFlagDTO {
    private Long id;
    private String flagType;
    private String detail;
    private LocalDateTime detectedAt;
    private int totalRelatedRequests;
}
