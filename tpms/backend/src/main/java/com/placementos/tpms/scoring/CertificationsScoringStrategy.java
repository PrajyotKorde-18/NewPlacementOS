package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CertificationsScoringStrategy implements ScoringStrategy {

    @Override
    public String getCategoryName() {
        return "certifications";
    }

    @Override
    public double calculateSubScore(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile) {
        if (studentProfile == null || studentProfile.getCertifications() == null || studentProfile.getCertifications().isEmpty()) {
            return 0.0;
        }

        List<Map<String, Object>> certs = studentProfile.getCertifications();
        int certCount = certs.size();

        // 40 points per certification up to max 100
        double score = certCount * 40.0;
        return Math.min(100.0, Math.max(0.0, score));
    }
}
