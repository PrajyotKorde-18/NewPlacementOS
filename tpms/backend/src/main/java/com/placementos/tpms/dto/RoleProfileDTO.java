package com.placementos.tpms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleProfileDTO {
    private Long id;
    private Long driveId;

    // Required Skills with importance buckets: MUST_HAVE, NICE_TO_HAVE, BONUS
    // e.g. {"MUST_HAVE": ["Java", "Spring Boot"], "NICE_TO_HAVE": ["Docker", "MySQL"], "BONUS": ["AWS"]}
    private Map<String, List<String>> requiredSkills;

    // Category weights for the 5 sub-score categories (sum = 1.0 or 100%)
    // e.g. {"skillMatch": 0.4, "projectRelevance": 0.25, "codingActivity": 0.15, "certifications": 0.1, "experience": 0.1}
    private Map<String, Double> weights;

    // Academic Criteria & Drive Info (optional / extracted via AI)
    private Integer vacancies;
    private Double minCgpa;
    private Double minTenthPercentage;
    private Double minTwelfthPercentage;
    private Integer maxActiveBacklogs;
}
