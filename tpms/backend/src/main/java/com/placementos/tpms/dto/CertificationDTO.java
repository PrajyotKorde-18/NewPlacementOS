package com.placementos.tpms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationDTO {

    private Long id;

    @NotBlank(message = "Certification name is required")
    private String name;

    @NotBlank(message = "Issuing body is required")
    private String issuingBody;

    private LocalDate issueDate;

    @Pattern(
        regexp = "^$|^(https?://)?([a-zA-Z0-9.-]+)(:[0-9]+)?(/.*)?$",
        message = "Credential URL must be a valid URL"
    )
    private String credentialUrl;
}
