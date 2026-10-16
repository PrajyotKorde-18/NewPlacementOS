package com.placementos.internship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrConfirmActionDTO {
    @NotBlank(message = "Answer is mandatory")
    @Pattern(regexp = "(?i)^(YES|NO)$", message = "Answer must be either 'YES' or 'NO'")
    private String answer;
}
