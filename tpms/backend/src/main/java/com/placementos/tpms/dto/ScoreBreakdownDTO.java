package com.placementos.tpms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreBreakdownDTO {
    private Double skillMatchScore;        // 0.0 - 100.0
    private Double projectRelevanceScore;   // 0.0 - 100.0
    private Double codingActivityScore;    // 0.0 - 100.0
    private Double certificationsScore;    // 0.0 - 100.0
    private Double experienceScore;        // 0.0 - 100.0
    private Double totalWeightedScore;     // 0.0 - 100.0
}
