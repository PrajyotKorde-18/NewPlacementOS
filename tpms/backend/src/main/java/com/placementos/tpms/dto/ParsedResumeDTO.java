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
public class ParsedResumeDTO {
    private List<String> skills;
    private List<Map<String, Object>> projects;
    private List<Map<String, Object>> internships;
    private List<Map<String, Object>> certifications;
    private Map<String, String> codingProfiles;
    private String summary;
    private String achievements;
    private String rawText;
}
