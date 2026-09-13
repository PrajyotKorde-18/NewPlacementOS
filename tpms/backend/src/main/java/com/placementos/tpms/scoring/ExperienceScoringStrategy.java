package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExperienceScoringStrategy implements ScoringStrategy {

    @Override
    public String getCategoryName() {
        return "experience";
    }

    @Override
    public double calculateSubScore(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile) {
        if (studentProfile == null || studentProfile.getInternships() == null || studentProfile.getInternships().isEmpty()) {
            return 0.0;
        }

        List<Map<String, Object>> internships = studentProfile.getInternships();
        int count = internships.size();

        // 50 points per internship/experience up to max 100
        double score = count * 50.0;
        return Math.min(100.0, Math.max(0.0, score));
    }
}
