package com.placementos.internship.service;

import com.placementos.internship.dto.*;
import com.placementos.internship.entity.*;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.exception.ResourceNotFoundException;
import com.placementos.internship.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placementos.internship.event.InternshipApprovedEvent;
import com.placementos.internship.event.InternshipRejectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CdpcReviewService {

    private final InternshipRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NocRecordRepository nocRecordRepository;
    private final NocPdfGeneratorService nocPdfGeneratorService;
    private final NocVerificationTokenService nocVerificationTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final HrConfirmationRepository hrConfirmationRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public CdpcReviewService(
            InternshipRequestRepository requestRepository,
            UserRepository userRepository,
            NocRecordRepository nocRecordRepository,
            NocPdfGeneratorService nocPdfGeneratorService,
            NocVerificationTokenService nocVerificationTokenService,
            ApplicationEventPublisher eventPublisher,
            @org.springframework.beans.factory.annotation.Autowired(required = false) HrConfirmationRepository hrConfirmationRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.nocRecordRepository = nocRecordRepository;
        this.nocPdfGeneratorService = nocPdfGeneratorService;
        this.nocVerificationTokenService = nocVerificationTokenService != null ? nocVerificationTokenService : new NocVerificationTokenService();
        this.eventPublisher = eventPublisher != null ? eventPublisher : event -> {};
        this.hrConfirmationRepository = hrConfirmationRepository;
    }

    public CdpcReviewService(
            InternshipRequestRepository requestRepository,
            UserRepository userRepository,
            NocRecordRepository nocRecordRepository,
            NocPdfGeneratorService nocPdfGeneratorService) {
        this(requestRepository, userRepository, nocRecordRepository, nocPdfGeneratorService, null, null, null);
    }

    private static final List<InternshipRequestStatus> PENDING_QUEUE_STATUSES = List.of(
            InternshipRequestStatus.SUBMITTED,
            InternshipRequestStatus.RULE_APPROVED,
            InternshipRequestStatus.RULE_REJECTED,
            InternshipRequestStatus.FLAGGED_FOR_REVIEW
    );

    @Transactional(readOnly = true)
    public List<InternshipResponseDTO> getReviewQueue() {
        List<InternshipRequest> pendingRequests = requestRepository.findByStatusIn(PENDING_QUEUE_STATUSES);
        return pendingRequests.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public InternshipResponseDTO approveRequest(Long requestId) throws IOException {
        User approver = getAuthenticatedCdpcUser();

        InternshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship request not found with id: " + requestId));

        // Terminal status guard check
        if (request.getStatus() == InternshipRequestStatus.MANUALLY_APPROVED ||
            request.getStatus() == InternshipRequestStatus.MANUALLY_REJECTED) {
            throw new AppException("Internship request #" + requestId + " is already in terminal state '" +
                    request.getStatus() + "' and cannot be re-actioned.", HttpStatus.CONFLICT);
        }

        // Employer HR denial guard check: If HR denied this offer, approval is strictly blocked
        if (hrConfirmationRepository != null) {
            hrConfirmationRepository.findByRequestId(requestId).ifPresent(hrConf -> {
                if (hrConf.getStatus() == HrConfirmationStatus.DENIED) {
                    throw new AppException("Cannot approve internship offer #" + requestId +
                            ": The employer HR contact has explicitly denied issuing this offer. Only rejection is permitted.",
                            HttpStatus.BAD_REQUEST);
                }
            });
        }

        // Explicit human action transitions to MANUALLY_APPROVED
        request.setStatus(InternshipRequestStatus.MANUALLY_APPROVED);
        request = requestRepository.save(request);

        // Generate HMAC-SHA256 verification token
        LocalDateTime approvedAt = LocalDateTime.now();
        String studentEmail = request.getStudent() != null && request.getStudent().getUser() != null
                ? request.getStudent().getUser().getEmail() : "";
        String token = nocVerificationTokenService.generateToken(request.getId(), studentEmail, request.getCompanyName(), approvedAt);

        // Generate NOC PDF using Apache PDFBox with embedded verification QR code
        String pdfPath = nocPdfGeneratorService.generateNocPdf(request, approver, token);

        // Save NOC Record
        NocRecord nocRecord = NocRecord.builder()
                .request(request)
                .approvedBy(approver)
                .approvedAt(approvedAt)
                .pdfPath(pdfPath)
                .verificationToken(token)
                .qrGeneratedAt(approvedAt)
                .build();
        nocRecord = nocRecordRepository.save(nocRecord);

        // Publish event for transactional notification listener
        eventPublisher.publishEvent(new InternshipApprovedEvent(request.getId(), nocRecord.getId()));

        log.info("[CDPC ACTION] Internship Request #{} MANUALLY_APPROVED by authenticated CDPC staff '{}' (ID: {}, Email: {}, Role: {}). NOC generated at '{}'.",
                request.getId(), approver.getName(), approver.getId(), approver.getEmail(), approver.getRole(), pdfPath);

        return mapToResponseDTO(request);
    }

    @Transactional
    public InternshipResponseDTO rejectRequest(Long requestId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new AppException("Rejection reason is mandatory and cannot be blank", HttpStatus.BAD_REQUEST);
        }

        User actioner = getAuthenticatedCdpcUser();

        InternshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship request not found with id: " + requestId));

        // Terminal status guard check
        if (request.getStatus() == InternshipRequestStatus.MANUALLY_APPROVED ||
            request.getStatus() == InternshipRequestStatus.MANUALLY_REJECTED) {
            throw new AppException("Internship request #" + requestId + " is already in terminal state '" +
                    request.getStatus() + "' and cannot be re-actioned.", HttpStatus.CONFLICT);
        }

        // Explicit human action transitions to MANUALLY_REJECTED
        request.setStatus(InternshipRequestStatus.MANUALLY_REJECTED);
        request.setRejectionReason(rejectionReason.trim());
        request = requestRepository.save(request);

        // Publish event for transactional notification listener
        eventPublisher.publishEvent(new InternshipRejectedEvent(request.getId(), rejectionReason.trim()));

        log.info("[CDPC ACTION] Internship Request #{} MANUALLY_REJECTED by authenticated CDPC staff '{}' (ID: {}, Email: {}, Role: {}). Reason: '{}'.",
                request.getId(), actioner.getName(), actioner.getId(), actioner.getEmail(), actioner.getRole(), rejectionReason.trim());

        return mapToResponseDTO(request);
    }

    @Transactional(readOnly = true)
    public NocRecordDTO getNocRecordByRequestId(Long requestId) {
        NocRecord noc = nocRecordRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("NOC record not found for request id: " + requestId));

        return NocRecordDTO.builder()
                .id(noc.getId())
                .requestId(noc.getRequest().getId())
                .approvedByName(noc.getApprovedBy() != null ? noc.getApprovedBy().getName() : null)
                .approvedAt(noc.getApprovedAt())
                .pdfPath(noc.getPdfPath())
                .verificationToken(noc.getVerificationToken())
                .qrGeneratedAt(noc.getQrGeneratedAt())
                .build();
    }

    private User getAuthenticatedCdpcUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("Authentication required to action CDPC review", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated CDPC user not found with email: " + email));

        validateCdpcAuthority(user);
        return user;
    }

    private void validateCdpcAuthority(User user) {
        if (user.getRole() != Role.CDPC_STAFF && user.getRole() != Role.CDPC_ADMIN) {
            throw new AppException("Access denied: User does not have CDPC staff permissions", HttpStatus.FORBIDDEN);
        }
    }

    private InternshipResponseDTO mapToResponseDTO(InternshipRequest request) {
        List<ScamFlagDTO> flagDTOs = null;
        if (request.getScamFlags() != null && !request.getScamFlags().isEmpty()) {
            flagDTOs = request.getScamFlags().stream()
                    .map(f -> ScamFlagDTO.builder()
                            .id(f.getId())
                            .flagType(f.getFlagType())
                            .detail(f.getDetail())
                            .detectedAt(f.getDetectedAt())
                            .totalRelatedRequests(f.getInternshipRequests() != null ? f.getInternshipRequests().size() : 0)
                            .build())
                    .collect(Collectors.toList());
        }

        String hrStatus = null;
        Integer hrResendCount = null;
        LocalDateTime hrSentAt = null;
        LocalDateTime hrRespondedAt = null;
        if (hrConfirmationRepository != null) {
            var hrConf = hrConfirmationRepository.findByRequestId(request.getId()).orElse(null);
            if (hrConf != null) {
                hrStatus = hrConf.getStatus() != null ? hrConf.getStatus().name() : null;
                hrResendCount = hrConf.getResendCount();
                hrSentAt = hrConf.getSentAt();
                hrRespondedAt = hrConf.getRespondedAt();
            }
        }

        return InternshipResponseDTO.builder()
                .id(request.getId())
                .studentId(request.getStudent() != null ? request.getStudent().getId() : null)
                .studentName(request.getStudent() != null && request.getStudent().getUser() != null ? request.getStudent().getUser().getName() : null)
                .collegeId(request.getStudent() != null ? request.getStudent().getCollegeId() : null)
                .companyName(request.getCompanyName())
                .roleTitle(request.getRoleTitle())
                .durationWeeks(request.getDurationWeeks())
                .stipend(request.getStipend())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .mode(request.getMode())
                .hrContactEmail(request.getHrContactEmail())
                .offerLetterUrl(request.getOfferLetterUrl())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .scamFlags(flagDTOs)
                .hrConfirmationStatus(hrStatus)
                .hrConfirmationResendCount(hrResendCount)
                .hrConfirmationSentAt(hrSentAt)
                .hrConfirmationRespondedAt(hrRespondedAt)
                .build();
    }
}
