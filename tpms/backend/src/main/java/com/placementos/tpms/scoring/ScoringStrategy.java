package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;

public interface ScoringStrategy {
    
    String getCategoryName();

    /**
     * Calculates category sub-score normalized between 0.0 and 100.0
     */
    double calculateSubScore(ParsedResumeDTO studentProfile, RoleProfileDTO roleProfile);
}
