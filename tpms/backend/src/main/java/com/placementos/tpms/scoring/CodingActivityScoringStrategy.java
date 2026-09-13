package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CodingActivityScoringStrategy implements ScoringStrategy {

    @Override
    public String getCategoryName() {
        return "codingActivity";
    }

    @Override
    public double calculateSubScore(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile) {
        if (studentProfile == null || studentProfile.getCodingProfiles() == null || studentProfile.getCodingProfiles().isEmpty()) {
            return 0.0;
        }

        Map<String, String> profiles = studentProfile.getCodingProfiles();
        double score = 0.0;

        if (profiles.containsKey("github") && profiles.get("github") != null) {
            score += 40.0;
        }
        if (profiles.containsKey("leetcode") && profiles.get("leetcode") != null) {
            score += 30.0;
        }
        if (profiles.containsKey("codeforces") && profiles.get("codeforces") != null) {
            score += 20.0;
        }
        if (profiles.containsKey("hackerrank") && profiles.get("hackerrank") != null) {
            score += 10.0;
        }

        return Math.min(100.0, Math.max(0.0, score));
    }
}
