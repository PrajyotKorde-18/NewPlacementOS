package com.placementos.tpms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.RoleProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JdExtractionService {

    private final PdfParserService pdfParserService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.groq.api-key:mock_key}")
    private String apiKey;

    @Value("${app.groq.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqUrl;

    @Value("${app.groq.model:groq/compound-mini}")
    private String groqModel;

    public RoleProfileDTO extractJdFromText(String jdText) throws Exception {
        if (jdText == null || jdText.trim().isEmpty()) {
            return buildDefaultRoleProfile();
        }

        if (mockMode && ("mock_key".equalsIgnoreCase(apiKey) || apiKey == null || apiKey.trim().isEmpty())) {
            log.info("[Mock Mode Enabled] Returning simulated Groq LLM JD classification preview");
            return extractMockedJdData(jdText);
        }

        return callGroqForJdExtractionWithRetry(jdText);
    }

    public RoleProfileDTO extractJdFromPdf(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            String rawText = pdfParserService.extractTextFromPdf(is);
            return extractJdFromText(rawText);
        }
    }

    private RoleProfileDTO callGroqForJdExtractionWithRetry(String jdText) throws Exception {
        List<String> keys = Arrays.stream(apiKey.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        Exception lastException = null;

        for (String singleKey : keys) {
            if ("mock_key".equalsIgnoreCase(singleKey)) continue;
            try {
                log.info("Calling Groq API for JD Extraction with key prefix [{}]", 
                        singleKey.substring(0, Math.min(10, singleKey.length())));
                return callGroqForJdExtraction(jdText, singleKey);
            } catch (Exception e) {
                lastException = e;
                log.warn("Groq API key attempt failed: {}", e.getMessage());
            }
        }

        log.warn("Groq API JD extraction failed for all keys. Falling back to simulated extraction: {}", 
                lastException != null ? lastException.getMessage() : "No valid API keys configured");
        return extractMockedJdData(jdText);
    }

    private RoleProfileDTO callGroqForJdExtraction(String jdText, String activeApiKey) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(activeApiKey);

        String systemPrompt = """
                You are an expert Job Description parsing AI. Analyze the provided Job Description text and extract:
                1. Skills classified into three categories:
                   - MUST_HAVE: core essential technical skills required for the role
                   - NICE_TO_HAVE: secondary or beneficial skills
                   - BONUS: good-to-have extra skills or tools
                2. Category weights summing to 1.0 (skillMatch, projectRelevance, codingActivity, certifications, experience).
                3. Academic Eligibility Criteria & Vacancies (extract numeric values ONLY if explicitly specified in the JD, otherwise null):
                   - vacancies: e.g. 5 or null
                   - minCgpa: e.g. 7.5 or null
                   - minTenthPercentage: e.g. 70.0 or null
                   - minTwelfthPercentage: e.g. 70.0 or null
                   - maxActiveBacklogs: e.g. 1 or null

                Return a single valid JSON object containing:
                1. "requiredSkills": object with keys "MUST_HAVE" (array), "NICE_TO_HAVE" (array), and "BONUS" (array).
                2. "weights": object with keys "skillMatch", "projectRelevance", "codingActivity", "certifications", and "experience".
                3. "vacancies": integer or null
                4. "minCgpa": number or null
                5. "minTenthPercentage": number or null
                6. "minTwelfthPercentage": number or null
                7. "maxActiveBacklogs": integer or null
                """;

        Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMessage = Map.of("role", "user", "content", "Job Description Text:\n" + jdText);
        Map<String, Object> responseFormat = Map.of("type", "json_object");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("response_format", responseFormat);
        requestBody.put("temperature", 0.1);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(groqUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
            String jsonContent = contentNode.asText();

            JsonNode parsedJson = objectMapper.readTree(jsonContent);

            Map<String, List<String>> requiredSkills = objectMapper.convertValue(parsedJson.path("requiredSkills"), new TypeReference<>() {});
            Map<String, Double> weights = objectMapper.convertValue(parsedJson.path("weights"), new TypeReference<>() {});

            requiredSkills = normalizeRequiredSkills(requiredSkills);
            weights = normalizeWeights(weights);

            Integer vacancies = parsedJson.hasNonNull("vacancies") ? parsedJson.path("vacancies").asInt() : null;
            Double minCgpa = parsedJson.hasNonNull("minCgpa") ? parsedJson.path("minCgpa").asDouble() : null;
            Double minTenth = parsedJson.hasNonNull("minTenthPercentage") ? parsedJson.path("minTenthPercentage").asDouble() : null;
            Double minTwelfth = parsedJson.hasNonNull("minTwelfthPercentage") ? parsedJson.path("minTwelfthPercentage").asDouble() : null;
            Integer maxBacklogs = parsedJson.hasNonNull("maxActiveBacklogs") ? parsedJson.path("maxActiveBacklogs").asInt() : null;

            return RoleProfileDTO.builder()
                    .requiredSkills(requiredSkills)
                    .weights(weights)
                    .vacancies(vacancies)
                    .minCgpa(minCgpa)
                    .minTenthPercentage(minTenth)
                    .minTwelfthPercentage(minTwelfth)
                    .maxActiveBacklogs(maxBacklogs)
                    .build();
        } else {
            throw new RuntimeException("Groq API returned HTTP status: " + response.getStatusCode());
        }
    }

    private Map<String, List<String>> normalizeRequiredSkills(Map<String, List<String>> input) {
        Map<String, List<String>> result = new HashMap<>();
        if (input != null) {
            result.putAll(input);
        }
        result.putIfAbsent("MUST_HAVE", Collections.emptyList());
        result.putIfAbsent("NICE_TO_HAVE", Collections.emptyList());
        result.putIfAbsent("BONUS", Collections.emptyList());
        return result;
    }

    private Map<String, Double> normalizeWeights(Map<String, Double> input) {
        Map<String, Double> defaults = getDefaultWeights();
        if (input == null || input.isEmpty()) {
            return defaults;
        }

        Map<String, Double> weights = new HashMap<>();
        weights.put("skillMatch", input.getOrDefault("skillMatch", defaults.get("skillMatch")));
        weights.put("projectRelevance", input.getOrDefault("projectRelevance", defaults.get("projectRelevance")));
        weights.put("codingActivity", input.getOrDefault("codingActivity", defaults.get("codingActivity")));
        weights.put("certifications", input.getOrDefault("certifications", defaults.get("certifications")));
        weights.put("experience", input.getOrDefault("experience", defaults.get("experience")));

        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum < 0.98 || sum > 1.02) {
            if (sum <= 0) {
                return defaults;
            }
            final double actualSum = sum;
            weights.replaceAll((key, val) -> Math.round((val / actualSum) * 100.0) / 100.0);
        }
        return weights;
    }

    private RoleProfileDTO extractMockedJdData(String jdText) {
        Map<String, List<String>> requiredSkills = Map.of(
                "MUST_HAVE", List.of("Java", "Spring Boot", "REST API"),
                "NICE_TO_HAVE", List.of("MySQL", "Docker", "Git"),
                "BONUS", List.of("AWS", "Kubernetes")
        );

        return RoleProfileDTO.builder()
                .requiredSkills(normalizeRequiredSkills(requiredSkills))
                .weights(normalizeWeights(getDefaultWeights()))
                .minCgpa(7.5)
                .minTenthPercentage(70.0)
                .minTwelfthPercentage(70.0)
                .maxActiveBacklogs(1)
                .build();
    }

    private RoleProfileDTO buildDefaultRoleProfile() {
        return RoleProfileDTO.builder()
                .requiredSkills(normalizeRequiredSkills(Collections.emptyMap()))
                .weights(getDefaultWeights())
                .build();
    }

    private Map<String, Double> getDefaultWeights() {
        return Map.of(
                "skillMatch", 0.40,
                "projectRelevance", 0.25,
                "codingActivity", 0.15,
                "certifications", 0.10,
                "experience", 0.10
        );
    }
}
