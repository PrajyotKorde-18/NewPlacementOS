package com.placementos.tpms.dto;

import com.placementos.tpms.entity.ExtractionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeUploadResponse {
    private Long resumeId;
    private Long versionId;
    private Long studentId;
    private String filePath;
    private ExtractionStatus extractionStatus;
    private String message;
    private ParsedResumeDTO extractedData;
}
