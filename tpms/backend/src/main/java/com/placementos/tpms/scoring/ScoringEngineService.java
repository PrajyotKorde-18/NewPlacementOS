package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import com.placementos.tpms.dto.ScoreBreakdownDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringEngineService {

    private final List<ScoringStrategy> scoringStrategies;

    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            "skillMatch", 0.35,
            "projectRelevance", 0.25,
            "codingActivity", 0.15,
            "certifications", 0.10,
            "experience", 0.15
    );

    public ScoreBreakdownDTO computeScoreBreakdown(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile) {
        Map<String, Double> configuredWeights = (roleProfile != null && roleProfile.getWeights() != null && !roleProfile.getWeights().isEmpty())
                ? roleProfile.getWeights()
                : DEFAULT_WEIGHTS;

        Map<String, Double> subScores = new HashMap<>();

        for (ScoringStrategy strategy : scoringStrategies) {
            double subScore = strategy.calculateSubScore(studentProfile, roleProfile);
            subScores.put(strategy.getCategoryName(), subScore);
        }

        double skillMatchScore = subScores.getOrDefault("skillMatch", 0.0);
        double projectRelevanceScore = subScores.getOrDefault("projectRelevance", 0.0);
        double codingActivityScore = subScores.getOrDefault("codingActivity", 0.0);
        double certificationsScore = subScores.getOrDefault("certifications", 0.0);
        double experienceScore = subScores.getOrDefault("experience", 0.0);

        double wSkill = configuredWeights.getOrDefault("skillMatch", DEFAULT_WEIGHTS.get("skillMatch"));
        double wProject = configuredWeights.getOrDefault("projectRelevance", DEFAULT_WEIGHTS.get("projectRelevance"));
        double wCoding = configuredWeights.getOrDefault("codingActivity", DEFAULT_WEIGHTS.get("codingActivity"));
        double wCert = configuredWeights.getOrDefault("certifications", DEFAULT_WEIGHTS.get("certifications"));
        double wExp = configuredWeights.getOrDefault("experience", DEFAULT_WEIGHTS.get("experience"));

        double totalWeight = wSkill + wProject + wCoding + wCert + wExp;
        if (totalWeight <= 0) totalWeight = 1.0;

        double weightedSum = (skillMatchScore * wSkill)
                + (projectRelevanceScore * wProject)
                + (codingActivityScore * wCoding)
                + (certificationsScore * wCert)
                + (experienceScore * wExp);

        double totalWeightedScore = Math.round((weightedSum / totalWeight) * 100.0) / 100.0;

        return ScoreBreakdownDTO.builder()
                .skillMatchScore(skillMatchScore)
                .projectRelevanceScore(projectRelevanceScore)
                .codingActivityScore(codingActivityScore)
                .certificationsScore(certificationsScore)
                .experienceScore(experienceScore)
                .totalWeightedScore(totalWeightedScore)
                .build();
    }
}
