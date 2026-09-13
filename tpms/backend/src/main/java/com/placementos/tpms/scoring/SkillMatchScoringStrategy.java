package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SkillMatchScoringStrategy implements ScoringStrategy {

    @Override
    public String getCategoryName() {
        return "skillMatch";
    }

    @Override
    public double calculateSubScore(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile) {
        if (studentProfile == null) {
            return 0.0;
        }

        boolean hasSkills = studentProfile.getSkills() != null && !studentProfile.getSkills().isEmpty();
        boolean hasSummary = studentProfile.getSummary() != null && !studentProfile.getSummary().trim().isEmpty();
        boolean hasAchievements = studentProfile.getAchievements() != null && !studentProfile.getAchievements().trim().isEmpty();

        if (!hasSkills && !hasSummary && !hasAchievements) {
            return 0.0;
        }

        Map<String, List<String>> requiredSkillsMap = roleProfile != null ? roleProfile.getRequiredSkills() : null;
        if (requiredSkillsMap == null || requiredSkillsMap.isEmpty()) {
            // Default baseline match if no role skills specified
            return 70.0;
        }

        Set<String> candidateSkills = new HashSet<>();
        if (studentProfile.getSkills() != null) {
            for (String s : studentProfile.getSkills()) {
                candidateSkills.add(s.trim().toLowerCase());
            }
        }

        String summaryText = studentProfile.getSummary() != null ? studentProfile.getSummary().toLowerCase() : "";
        String achievementsText = studentProfile.getAchievements() != null ? studentProfile.getAchievements().toLowerCase() : "";
        String combinedText = summaryText + " " + achievementsText;

        List<String> mustHave = requiredSkillsMap.getOrDefault("MUST_HAVE", Collections.emptyList());
        List<String> niceToHave = requiredSkillsMap.getOrDefault("NICE_TO_HAVE", Collections.emptyList());
        List<String> bonus = requiredSkillsMap.getOrDefault("BONUS", Collections.emptyList());

        double totalPointsPossible = (mustHave.size() * 3.0) + (niceToHave.size() * 2.0) + (bonus.size() * 1.0);
        if (totalPointsPossible == 0.0) {
            return 80.0;
        }

        double pointsEarned = 0.0;
        for (String skill : mustHave) {
            if (isSkillPresent(skill, candidateSkills, combinedText)) {
                pointsEarned += 3.0;
            }
        }
        for (String skill : niceToHave) {
            if (isSkillPresent(skill, candidateSkills, combinedText)) {
                pointsEarned += 2.0;
            }
        }
        for (String skill : bonus) {
            if (isSkillPresent(skill, candidateSkills, combinedText)) {
                pointsEarned += 1.0;
            }
        }

        double score = (pointsEarned / totalPointsPossible) * 100.0;
        return Math.min(100.0, Math.max(0.0, score));
    }

    private boolean isSkillPresent(String skill, Set<String> candidateSkills, String combinedText) {
        if (skill == null || skill.trim().isEmpty()) return false;
        String skillLower = skill.trim().toLowerCase();

        // 1. Direct match in candidate skills set
        if (candidateSkills.contains(skillLower)) {
            return true;
        }

        // 2. Strip parenthetical qualifiers (e.g., "Java (JDK 17+)" -> "java")
        String cleanSkill = skillLower.replaceAll("\\(.*\\)", "").trim();
        if (!cleanSkill.isEmpty() && candidateSkills.contains(cleanSkill)) {
            return true;
        }

        // 3. Bidirectional substring match against candidate skill list
        for (String candidateSkill : candidateSkills) {
            if (candidateSkill.length() >= 2) {
                if (skillLower.contains(candidateSkill) || candidateSkill.contains(skillLower)) {
                    return true;
                }
                if (!cleanSkill.isEmpty() && (cleanSkill.contains(candidateSkill) || candidateSkill.contains(cleanSkill))) {
                    return true;
                }
            }
        }

        // 4. Fallback search in summary / achievements prose text
        return !combinedText.isEmpty() && (combinedText.contains(skillLower) || (!cleanSkill.isEmpty() && combinedText.contains(cleanSkill)));
    }
}
