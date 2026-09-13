package com.placementos.internship.service;

import com.placementos.internship.dto.InternshipResponseDTO;
import com.placementos.internship.entity.*;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.NocRecordRepository;
import com.placementos.internship.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdpcReviewServiceTest {

    @Mock
    private InternshipRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NocRecordRepository nocRecordRepository;
    @Mock
    private NocPdfGeneratorService nocPdfGeneratorService;

    private CdpcReviewService cdpcReviewService;

    private User coordinatorUser;
    private User studentUser;
    private InternshipRequest ruleApprovedRequest;
    private InternshipRequest flaggedRequest;
    private InternshipRequest terminalApprovedRequest;
    private InternshipRequest terminalRejectedRequest;

    @BeforeEach
    void setUp() {
        cdpcReviewService = new CdpcReviewService(
                requestRepository,
                userRepository,
                nocRecordRepository,
                nocPdfGeneratorService
        );

        coordinatorUser = User.builder()
                .id(100L)
                .name("Coordinator Smith")
                .email("coordinator@rknec.edu")
                .role(Role.CDPC_STAFF)
                .collegeId("STAFF100")
                .build();

        studentUser = User.builder()
                .id(200L)
                .name("Bob Student")
                .email("bob@rknec.edu")
                .role(Role.STUDENT)
                .collegeId("STU200")
                .build();

        Student student = Student.builder().id(1L).user(studentUser).build();

        ruleApprovedRequest = InternshipRequest.builder()
                .id(10L)
                .student(student)
                .companyName("GoodCorp")
                .roleTitle("Engineer")
                .status(InternshipRequestStatus.RULE_APPROVED)
                .build();

        flaggedRequest = InternshipRequest.builder()
                .id(11L)
                .student(student)
                .companyName("ScamCorp")
                .roleTitle("Intern")
                .status(InternshipRequestStatus.FLAGGED_FOR_REVIEW)
                .build();

        terminalApprovedRequest = InternshipRequest.builder()
                .id(12L)
                .student(student)
                .companyName("DoneCorp")
                .roleTitle("Engineer")
                .status(InternshipRequestStatus.MANUALLY_APPROVED)
                .build();

        terminalRejectedRequest = InternshipRequest.builder()
                .id(13L)
                .student(student)
                .companyName("RejectedCorp")
                .roleTitle("Intern")
                .status(InternshipRequestStatus.MANUALLY_REJECTED)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                coordinatorUser.getEmail(), null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Review Queue: Returns RULE_APPROVED and FLAGGED_FOR_REVIEW requests together")
    void testGetReviewQueue_ReturnsFlaggedAndUnflaggedRequestsTogether() {
        when(requestRepository.findByStatusIn(anyList()))
                .thenReturn(List.of(ruleApprovedRequest, flaggedRequest));

        List<InternshipResponseDTO> queue = cdpcReviewService.getReviewQueue();

        assertEquals(2, queue.size());
        assertEquals(InternshipRequestStatus.RULE_APPROVED, queue.get(0).getStatus());
        assertEquals(InternshipRequestStatus.FLAGGED_FOR_REVIEW, queue.get(1).getStatus());
    }

    @Test
    @DisplayName("Approve Request: Derives staff identity from SecurityContext and generates NOC")
    void testApproveRequest_DerivesUserFromSecurityContextAndGeneratesNoc() throws Exception {
        when(userRepository.findByEmail("coordinator@rknec.edu")).thenReturn(Optional.of(coordinatorUser));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(ruleApprovedRequest));
        when(nocPdfGeneratorService.generateNocPdf(any(), any())).thenReturn("uploads/noc/NOC_10.pdf");
        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(i -> i.getArgument(0));

        InternshipResponseDTO response = cdpcReviewService.approveRequest(10L);

        assertNotNull(response);
        assertEquals(InternshipRequestStatus.MANUALLY_APPROVED, response.getStatus());
        verify(nocPdfGeneratorService, times(1)).generateNocPdf(any(), any());
        verify(nocRecordRepository, times(1)).save(any(NocRecord.class));
    }

    @Test
    @DisplayName("Terminal Status Guard: approveRequest rejects action on MANUALLY_APPROVED request")
    void testApproveRequest_TerminalStatusGuard_ThrowsConflictException() {
        when(userRepository.findByEmail("coordinator@rknec.edu")).thenReturn(Optional.of(coordinatorUser));
        when(requestRepository.findById(12L)).thenReturn(Optional.of(terminalApprovedRequest));

        AppException ex = assertThrows(AppException.class, () -> cdpcReviewService.approveRequest(12L));
        assertTrue(ex.getMessage().contains("already in terminal state"));
    }

    @Test
    @DisplayName("Terminal Status Guard: rejectRequest rejects action on MANUALLY_REJECTED request")
    void testRejectRequest_TerminalStatusGuard_ThrowsConflictException() {
        when(userRepository.findByEmail("coordinator@rknec.edu")).thenReturn(Optional.of(coordinatorUser));
        when(requestRepository.findById(13L)).thenReturn(Optional.of(terminalRejectedRequest));

        AppException ex = assertThrows(AppException.class, () -> cdpcReviewService.rejectRequest(13L, "Attempt second rejection"));
        assertTrue(ex.getMessage().contains("already in terminal state"));
    }

    @Test
    @DisplayName("Reject Request: Requires non-blank rejection reason")
    void testRejectRequest_RequiresNonBlankReason() {
        assertThrows(AppException.class, () ->
                cdpcReviewService.rejectRequest(10L, "   ")
        );
    }

    @Test
    @DisplayName("Reject Request: Derives staff identity from SecurityContext and sets MANUALLY_REJECTED")
    void testRejectRequest_DerivesUserFromSecurityContextAndSetsManuallyRejected() {
        when(userRepository.findByEmail("coordinator@rknec.edu")).thenReturn(Optional.of(coordinatorUser));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(ruleApprovedRequest));
        when(requestRepository.save(any(InternshipRequest.class))).thenAnswer(i -> i.getArgument(0));

        InternshipResponseDTO response = cdpcReviewService.rejectRequest(10L, "Company offer details invalid");

        assertNotNull(response);
        assertEquals(InternshipRequestStatus.MANUALLY_REJECTED, response.getStatus());
        assertEquals("Company offer details invalid", response.getRejectionReason());
    }

    @Test
    @DisplayName("Authority Validation: Denies approval action when authenticated caller is STUDENT")
    void testValidateCdpcAuthority_StudentRoleAccessDenied() {
        UsernamePasswordAuthenticationToken studentAuth = new UsernamePasswordAuthenticationToken(
                studentUser.getEmail(), null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(studentAuth);

        when(userRepository.findByEmail("bob@rknec.edu")).thenReturn(Optional.of(studentUser));

        AppException ex = assertThrows(AppException.class, () -> cdpcReviewService.approveRequest(10L));
        assertTrue(ex.getMessage().contains("Access denied"));
    }
}
