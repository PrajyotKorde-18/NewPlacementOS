package com.placementos.internship.service;

import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.ScamPatternFlag;
import com.placementos.internship.entity.Student;
import com.placementos.internship.entity.User;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.ScamPatternFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScamPatternDetectorServiceTest {

    @Mock
    private InternshipRequestRepository requestRepository;

    @Mock
    private ScamPatternFlagRepository scamPatternFlagRepository;

    @InjectMocks
    private ScamPatternDetectorService scamPatternDetectorService;

    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        User user1 = User.builder().id(10L).name("Alice").email("alice@rknec.edu").build();
        student1 = Student.builder().id(100L).user(user1).build();

        User user2 = User.builder().id(20L).name("Bob").email("bob@rknec.edu").build();
        student2 = Student.builder().id(200L).user(user2).build();
    }

    @Test
    @DisplayName("HR Domain Mismatch: Flags free email domains and skips legitimate corporate domains")
    void testHrDomainMismatch_FlagsFreeEmails_SkipsLegitimateDomains() {
        InternshipRequest freeEmailReq = InternshipRequest.builder()
                .id(1L)
                .student(student1)
                .companyName("Acme Tech")
                .hrContactEmail("hr_recruiter@gmail.com")
                .scamFlags(new HashSet<>())
                .build();

        when(scamPatternFlagRepository.findFirstByFlagTypeAndMatchKeyAndDetectedAtAfter(
                eq("HR_DOMAIN_MISMATCH"), eq("gmail.com"), any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        when(scamPatternFlagRepository.save(any(ScamPatternFlag.class))).thenAnswer(inv -> {
            ScamPatternFlag f = inv.getArgument(0);
            if (f.getId() == null) f.setId(50L);
            return f;
        });

        List<ScamPatternFlag> freeEmailFlags = scamPatternDetectorService.detectAndLinkPatternFlags(freeEmailReq);
        assertEquals(1, freeEmailFlags.size());
        assertEquals("HR_DOMAIN_MISMATCH", freeEmailFlags.get(0).getFlagType());
        assertEquals("gmail.com", freeEmailFlags.get(0).getMatchKey());

        // Legitimate company domain
        InternshipRequest legitEmailReq = InternshipRequest.builder()
                .id(2L)
                .student(student2)
                .companyName("Acme Tech")
                .hrContactEmail("hr@acmetech.com")
                .scamFlags(new HashSet<>())
                .build();

        List<ScamPatternFlag> legitFlags = scamPatternDetectorService.detectAndLinkPatternFlags(legitEmailReq);
        assertTrue(legitFlags.isEmpty(), "Legitimate corporate email domain should not trigger scam flag");
    }

    @Test
    @DisplayName("Duplicate Offer Hash Detection: Links multiple requests to ONE existing flag record using match_key")
    void testDuplicateOfferHash_LinksToSingleFlagRecord() {
        String sha256Hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        InternshipRequest req1 = InternshipRequest.builder()
                .id(10L)
                .student(student1)
                .companyName("Company Alpha")
                .offerLetterHash(sha256Hash)
                .scamFlags(new HashSet<>())
                .build();

        InternshipRequest req2 = InternshipRequest.builder()
                .id(20L)
                .student(student2)
                .companyName("Company Beta")
                .offerLetterHash(sha256Hash)
                .scamFlags(new HashSet<>())
                .build();

        // Previous request found in repository within 60 days
        when(requestRepository.findByOfferLetterHashAndCreatedAtAfter(eq(sha256Hash), any(LocalDateTime.class)))
                .thenReturn(List.of(req1));

        // Existing flag already created for student 1's submission
        ScamPatternFlag existingFlag = ScamPatternFlag.builder()
                .id(99L)
                .flagType("DUPLICATE_OFFER_LETTER_FILE")
                .matchKey(sha256Hash)
                .detectedAt(LocalDateTime.now().minusDays(2))
                .detail("Near-duplicate offer letter file hash detected across 1 student request(s)")
                .internshipRequests(new HashSet<>(Set.of(req1)))
                .build();

        when(scamPatternFlagRepository.findFirstByFlagTypeAndMatchKeyAndDetectedAtAfter(
                eq("DUPLICATE_OFFER_LETTER_FILE"), eq(sha256Hash), any(LocalDateTime.class)
        )).thenReturn(Optional.of(existingFlag));

        when(scamPatternFlagRepository.save(any(ScamPatternFlag.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ScamPatternFlag> flags = scamPatternDetectorService.detectAndLinkPatternFlags(req2);

        assertEquals(1, flags.size());
        ScamPatternFlag returnedFlag = flags.get(0);
        assertEquals(99L, returnedFlag.getId(), "Must reuse the existing flag record rather than creating a new one");
        assertEquals(sha256Hash, returnedFlag.getMatchKey());
        assertTrue(req2.getScamFlags().contains(returnedFlag));
        assertTrue(returnedFlag.getDetail().contains("across 2 distinct student submissions"));

        verify(scamPatternFlagRepository, times(1)).save(existingFlag);
    }

    @Test
    @DisplayName("Time Window Constraint: Offer match older than 60 days does NOT trigger a scam flag")
    void testTimeWindowConstraint_OlderThan60Days_DoesNotTriggerFlag() {
        String sha256Hash = "b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c";

        InternshipRequest currentReq = InternshipRequest.builder()
                .id(30L)
                .student(student1)
                .offerLetterHash(sha256Hash)
                .scamFlags(new HashSet<>())
                .build();

        // Repository returns empty list because query filters cutoffDate >= 60 days ago
        when(requestRepository.findByOfferLetterHashAndCreatedAtAfter(eq(sha256Hash), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<ScamPatternFlag> flags = scamPatternDetectorService.detectAndLinkPatternFlags(currentReq);

        assertTrue(flags.isEmpty(), "Matches older than 60 days cutoff must not trigger a pattern flag");

        // Verify lookup query was executed with a cutoff date around 60 days ago
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(requestRepository).findByOfferLetterHashAndCreatedAtAfter(eq(sha256Hash), cutoffCaptor.capture());

        LocalDateTime capturedCutoff = cutoffCaptor.getValue();
        assertTrue(capturedCutoff.isBefore(LocalDateTime.now().minusDays(59)));
        assertTrue(capturedCutoff.isAfter(LocalDateTime.now().minusDays(61)));
    }
}
