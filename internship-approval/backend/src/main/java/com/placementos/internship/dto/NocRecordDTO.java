package com.placementos.internship.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NocRecordDTO {
    private Long id;
    private Long requestId;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String pdfPath;
}
