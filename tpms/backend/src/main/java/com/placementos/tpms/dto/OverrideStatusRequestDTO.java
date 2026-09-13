package com.placementos.tpms.dto;

import com.placementos.tpms.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverrideStatusRequestDTO {

    @NotNull(message = "Status is required (must be OVERRIDDEN_INCLUDED or OVERRIDDEN_EXCLUDED)")
    private ApplicationStatus status;

    @NotBlank(message = "Mandatory non-empty override reason is required")
    private String overrideReason;
}
