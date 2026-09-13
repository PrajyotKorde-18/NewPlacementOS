package com.placementos.tpms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacklogRecordDTO {

    private Long id;

    @NotBlank(message = "Subject name is required")
    private String subjectName;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 10, message = "Semester cannot exceed 10")
    private Integer semester;

    @NotNull(message = "Cleared flag is required")
    @Builder.Default
    private Boolean cleared = false;

    private LocalDate clearedOn;
}
