package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProjectRelevanceScoringStrategy implements ScoringStrategy {

    @Override
    public String getCategoryName() {
        return "projectRelevance";
    }

    @Override
    public double calculateSubScore(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile) {
        if (studentProfile == null || studentProfile.getProjects() == null || studentProfile.getProjects().isEmpty()) {
            return 0.0;
        }

        List<Map<String, Object>> projects = studentProfile.getProjects();
        int projectCount = projects.size();

        // Baseline score for project count
        double baseScore = Math.min(60.0, projectCount * 30.0);

        // Check keyword matches in project titles and descriptions
        double relevanceBonus = 0.0;
        for (Map<String, Object> project : projects) {
            String title = String.valueOf(project.getOrDefault("title", "")).toLowerCase();
            String desc = String.valueOf(project.getOrDefault("description", "")).toLowerCase();
            String techStack = String.valueOf(project.getOrDefault("techStack", "")).toLowerCase();

            if (!techStack.isEmpty()) {
                relevanceBonus += 15.0;
            }
            if (title.contains("app") || title.contains("system") || title.contains("backend") || title.contains("api") || title.contains("web") || desc.contains("backend") || desc.contains("api")) {
                relevanceBonus += 10.0;
            }
        }

        double score = baseScore + Math.min(40.0, relevanceBonus);
        return Math.min(100.0, Math.max(0.0, score));
    }
}
