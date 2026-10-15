package com.placementos.internship.service;

import com.placementos.internship.dto.*;
import com.placementos.internship.entity.*;
import com.placementos.internship.evaluator.MinDurationRuleEvaluator;
import com.placementos.internship.evaluator.MinStipendRuleEvaluator;
import com.placementos.internship.evaluator.RuleEvaluatorService;
import com.placementos.internship.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternshipSubmissionServiceTest {

    @Mock
    private InternshipRequestRepository requestRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PolicyRuleRepository policyRuleRepository;
    @Mock
    private RuleEvaluationRepository ruleEvaluationRepository;
    @Mock
    private ScamPatternFlagRepository scamPatternFlagRepository;

    private RuleEvaluatorService ruleEvaluatorService;
    private ScamPatternDetectorService scamPatternDetectorService;
    private InternshipSubmissionService submissionService;

    private Student testStudent;
    private Student otherStudent;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Jane Student")
                .email("jane@rknec.edu")
                .collegeId("C100")
                .role(Role.STUDENT)
                .build();

        testStudent = Student.builder()
                .id(1L)
                .user(testUser)
                .department("CS")
                .build();

        otherUser = User.builder()
                .id(2L)
                .name("John Student")
                .email("john@rknec.edu")
                .collegeId("C101")
                .role(Role.STUDENT)
                .build();

        otherStudent = Student.builder()
                .id(2L)
                .user(otherUser)
                .department("CS")
                .build();

        MinStipendRuleEvaluator minStipendRuleEvaluator = new MinStipendRuleEvaluator();
        MinDurationRuleEvaluator minDurationRuleEvaluator = new MinDurationRuleEvaluator();

        ruleEvaluatorService = new RuleEvaluatorService(
                List.of(minStipendRuleEvaluator, minDurationRuleEvaluator),
                policyRuleRepository,
                ruleEvaluationRepository
        );

        scamPatternDetectorService = new ScamPatternDetectorService(
                requestRepository,
                scamPatternFlagRepository
        );

        submissionService = new InternshipSubmissionService(
                requestRepository,
                studentRepository,
                userRepository,
                ruleEvaluatorService,
                scamPatternDetectorService
        );
    }

    @Test
    @DisplayName("Non-Terminal Status Guard: Passing policy rules yields RULE_APPROVED status, requiring explicit human review")
    void testSubmitOffer_RuleApprovedStatus_NeverBypassesHumanReview() throws Exception {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        PolicyRule rule1 = PolicyRule.builder()
                .ruleName("MINIMUM_STIPEND")
                .ruleType("STIPEND")
                .ruleValue("5000")
                .label("Min Stipend")
                .isActive(true)
                .build();
        when(policyRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule1));

        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(i -> {
            InternshipRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(10L);
            return req;
        });

        InternshipSubmissionDTO dto = InternshipSubmissionDTO.builder()
                .studentId(1L)
                .companyName("Acme Corp")
                .roleTitle("Developer Intern")
                .durationWeeks(12)
                .stipend(new BigDecimal("10000"))
                .hrContactEmail("hr@acmecorp.com")
                .build();

        InternshipResponseDTO response = submissionService.submitOffer(dto, null);

        assertNotNull(response);
        assertEquals(InternshipRequestStatus.RULE_APPROVED, response.getStatus());
        assertNotEquals(InternshipRequestStatus.MANUALLY_APPROVED, response.getStatus());
        assertTrue(response.getRuleEvaluations().get(0).isPassed());
    }

    @Test
    @DisplayName("Non-Terminal Status Guard: Failing policy rule yields RULE_REJECTED non-terminal status, never auto-rejecting")
    void testSubmitOffer_RuleFailedStatus_ResultsInNonTerminalRuleRejected() throws Exception {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        PolicyRule rule1 = PolicyRule.builder()
                .ruleName("MINIMUM_STIPEND")
                .ruleType("STIPEND")
                .ruleValue("5000")
                .label("Min Stipend")
                .isActive(true)
                .build();
        when(policyRuleRepository.findByIsActiveTrue()).thenReturn(List.of(rule1));

        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(i -> {
            InternshipRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(15L);
            return req;
        });

        // Offer stipend is 3000, failing the 5000 threshold
        InternshipSubmissionDTO dto = InternshipSubmissionDTO.builder()
                .studentId(1L)
                .companyName("LowPay Inc")
                .roleTitle("Trainee")
                .durationWeeks(8)
                .stipend(new BigDecimal("3000"))
                .hrContactEmail("hr@lowpay.com")
                .build();

        InternshipResponseDTO response = submissionService.submitOffer(dto, null);

        assertNotNull(response);

        // MUST be RULE_REJECTED (non-terminal), NEVER MANUALLY_REJECTED (terminal)
        assertEquals(InternshipRequestStatus.RULE_REJECTED, response.getStatus());
        assertNotEquals(InternshipRequestStatus.MANUALLY_REJECTED, response.getStatus());
        assertFalse(response.getRuleEvaluations().get(0).isPassed());
    }

    @Test
    @DisplayName("HR Domain Mismatch: Triggers FLAGGED_FOR_REVIEW status")
    void testSubmitOffer_HRDomainMismatch_TriggersFlaggedForReview() throws Exception {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(policyRuleRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        when(scamPatternFlagRepository.save(any(ScamPatternFlag.class))).thenAnswer(i -> {
            ScamPatternFlag f = i.getArgument(0);
            if (f.getId() == null) f.setId(100L);
            return f;
        });

        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(i -> {
            InternshipRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(20L);
            return req;
        });

        InternshipSubmissionDTO dto = InternshipSubmissionDTO.builder()
                .studentId(1L)
                .companyName("TechCorp")
                .roleTitle("SWE Intern")
                .durationWeeks(8)
                .stipend(new BigDecimal("15000"))
                .hrContactEmail("techcorp_recruiter@gmail.com")
                .build();

        InternshipResponseDTO response = submissionService.submitOffer(dto, null);

        assertEquals(InternshipRequestStatus.FLAGGED_FOR_REVIEW, response.getStatus());
        assertNotNull(response.getScamFlags());
        assertEquals("HR_DOMAIN_MISMATCH", response.getScamFlags().get(0).getFlagType());
    }

    @Test
    @DisplayName("Duplicate Offer File: Triggers FLAGGED_FOR_REVIEW status")
    void testSubmitOffer_IndependentDuplicateOfferFile_TriggersScamFlag() throws Exception {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(policyRuleRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        InternshipRequest existingReq = InternshipRequest.builder()
                .id(99L)
                .student(otherStudent)
                .companyName("Unique Company")
                .hrContactEmail("hr@uniquecompany.com")
                .offerLetterHash("dummyhash123")
                .scamFlags(new HashSet<>())
                .build();

        when(scamPatternFlagRepository.save(any(ScamPatternFlag.class))).thenAnswer(inv -> {
            ScamPatternFlag f = inv.getArgument(0);
            if (f.getId() == null) f.setId(500L);
            return f;
        });

        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(inv -> {
            InternshipRequest req = inv.getArgument(0);
            if (req.getId() == null) req.setId(21L);
            return req;
        });

        byte[] validPdfBytes = "%PDF-1.4\ndummy content".getBytes();
        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "file", "offer.pdf", "application/pdf", validPdfBytes
        );

        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(validPdfBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) hexString.append(String.format("%02x", b));
        String expectedHash = hexString.toString();

        when(requestRepository.findByOfferLetterHashAndCreatedAtAfter(eq(expectedHash), any(LocalDateTime.class)))
                .thenReturn(List.of(existingReq));

        InternshipSubmissionDTO dto = InternshipSubmissionDTO.builder()
                .studentId(1L)
                .companyName("Unique Company")
                .roleTitle("Intern")
                .durationWeeks(6)
                .stipend(new BigDecimal("5000"))
                .hrContactEmail("hr@uniquecompany.com")
                .build();

        InternshipResponseDTO response = submissionService.submitOffer(dto, mockFile);

        assertEquals(InternshipRequestStatus.FLAGGED_FOR_REVIEW, response.getStatus());
        assertNotNull(response.getScamFlags());
        assertEquals("DUPLICATE_OFFER_LETTER_FILE", response.getScamFlags().get(0).getFlagType());
    }

    @Test
    @DisplayName("Zero Active Rules Guard: If zero active policy rules are configured, flag submission for review")
    void testSubmitOffer_ZeroActiveRules_FlagsForReviewWithReason() throws Exception {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(policyRuleRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(inv -> {
            InternshipRequest req = inv.getArgument(0);
            if (req.getId() == null) req.setId(30L);
            return req;
        });

        InternshipSubmissionDTO dto = InternshipSubmissionDTO.builder()
                .studentId(1L)
                .companyName("ValidCorp")
                .roleTitle("SWE Intern")
                .durationWeeks(8)
                .stipend(new BigDecimal("25000"))
                .hrContactEmail("recruiter@validcorp.com")
                .build();

        InternshipResponseDTO response = submissionService.submitOffer(dto, null);

        assertEquals(InternshipRequestStatus.FLAGGED_FOR_REVIEW, response.getStatus());
        assertEquals("no active policy rules configured", response.getRejectionReason());
    }
}
