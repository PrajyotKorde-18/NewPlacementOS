package com.placementos.internship.service;

import com.placementos.internship.dto.*;
import com.placementos.internship.entity.*;
import com.placementos.internship.evaluator.RuleEvaluatorService;
import com.placementos.internship.exception.ResourceNotFoundException;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InternshipSubmissionService {

    private final InternshipRequestRepository requestRepository;
    private final StudentRepository studentRepository;
    private final com.placementos.internship.repository.UserRepository userRepository;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final ScamPatternDetectorService scamPatternDetectorService;
    private final com.placementos.internship.repository.NocRecordRepository nocRecordRepository;

    public InternshipSubmissionService(
            InternshipRequestRepository requestRepository,
            StudentRepository studentRepository,
            com.placementos.internship.repository.UserRepository userRepository,
            RuleEvaluatorService ruleEvaluatorService,
            ScamPatternDetectorService scamPatternDetectorService
    ) {
        this(requestRepository, studentRepository, userRepository, ruleEvaluatorService, scamPatternDetectorService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public InternshipSubmissionService(
            InternshipRequestRepository requestRepository,
            StudentRepository studentRepository,
            com.placementos.internship.repository.UserRepository userRepository,
            RuleEvaluatorService ruleEvaluatorService,
            ScamPatternDetectorService scamPatternDetectorService,
            com.placementos.internship.repository.NocRecordRepository nocRecordRepository
    ) {
        this.requestRepository = requestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.ruleEvaluatorService = ruleEvaluatorService;
        this.scamPatternDetectorService = scamPatternDetectorService;
        this.nocRecordRepository = nocRecordRepository;
    }

    private static final String UPLOAD_DIR = "uploads/offers/";

    private Student resolveStudent(Long studentId) {
        if (studentId != null) {
            java.util.Optional<Student> byId = studentRepository.findById(studentId);
            if (byId.isPresent()) return byId.get();

            java.util.Optional<Student> byUser = studentRepository.findByUserId(studentId);
            if (byUser.isPresent()) return byUser.get();

            User user = userRepository.findById(studentId).orElse(null);
            if (user != null) {
                Student newStudent = Student.builder()
                        .user(user)
                        .department("Computer Science")
                        .gpa(8.5)
                        .build();
                return studentRepository.save(newStudent);
            }
        }

        return studentRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
    }

    @Transactional
    public InternshipResponseDTO submitOffer(InternshipSubmissionDTO dto, MultipartFile offerLetterFile) throws IOException {
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Internship end date cannot be before start date.");
        }

        Student student = resolveStudent(dto.getStudentId());

        String pdfPath = null;
        String fileHash = null;
        if (offerLetterFile != null && !offerLetterFile.isEmpty()) {
            com.placementos.internship.util.FileUploadValidator.validateFile(
                    offerLetterFile,
                    com.placementos.internship.util.FileUploadValidator.ALLOWED_OFFER_EXTENSIONS,
                    com.placementos.internship.util.FileUploadValidator.DEFAULT_MAX_SIZE_BYTES
            );

            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String sanitized = com.placementos.internship.util.FileUploadValidator.sanitizeFilename(offerLetterFile.getOriginalFilename());
            String fileName = UUID.randomUUID() + "_" + sanitized;
            Path targetPath = Paths.get(UPLOAD_DIR + fileName);
            byte[] bytes = offerLetterFile.getBytes();
            Files.write(targetPath, bytes);
            pdfPath = targetPath.toString();

            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(bytes);
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    hexString.append(String.format("%02x", b));
                }
                fileHash = hexString.toString();
            } catch (Exception ignored) {
            }
        }

        // 1. Initial creation with SUBMITTED status
        InternshipRequest request = InternshipRequest.builder()
                .student(student)
                .companyName(dto.getCompanyName())
                .roleTitle(dto.getRoleTitle())
                .durationWeeks(dto.getDurationWeeks())
                .stipend(dto.getStipend())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .mode(dto.getMode() != null ? dto.getMode() : InternshipMode.ON_SITE)
                .hrContactEmail(dto.getHrContactEmail())
                .offerLetterUrl(pdfPath)
                .offerLetterHash(fileHash)
                .status(InternshipRequestStatus.SUBMITTED)
                .build();

        request = requestRepository.save(request);

        // 2. Run pluggable Policy Rules Evaluator
        List<RuleEvaluationResultDTO> ruleResults = ruleEvaluatorService.evaluateAll(request);
        boolean allRulesPassed = ruleResults.stream().allMatch(RuleEvaluationResultDTO::isPassed);

        // 3. Run Cross-Request Scam Pattern Detector
        List<ScamPatternFlag> scamFlags = scamPatternDetectorService.detectAndLinkPatternFlags(request);

        // 4. Assign Intermediate Non-Terminal Status (Pending CDPC Human Review)
        if (!scamFlags.isEmpty()) {
            request.setStatus(InternshipRequestStatus.FLAGGED_FOR_REVIEW);
        } else if (ruleResults.isEmpty()) {
            request.setStatus(InternshipRequestStatus.FLAGGED_FOR_REVIEW);
            request.setRejectionReason("no active policy rules configured");
        } else if (allRulesPassed) {
            request.setStatus(InternshipRequestStatus.RULE_APPROVED);
        } else {
            request.setStatus(InternshipRequestStatus.RULE_REJECTED);
        }

        request = requestRepository.save(request);

        return mapToResponseDTO(request, ruleResults, scamFlags);
    }

    @Transactional(readOnly = true)
    public InternshipResponseDTO getRequestById(Long requestId) {
        InternshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship request not found with id: " + requestId));
        return mapToResponseDTO(request, null, null);
    }

    @Transactional(readOnly = true)
    public List<InternshipResponseDTO> getRequestsByStudent(Long studentId) {
        Student student = resolveStudent(studentId);
        List<InternshipRequest> requests = requestRepository.findByStudentId(student.getId());
        return requests.stream()
                .map(req -> mapToResponseDTO(req, null, null))
                .collect(Collectors.toList());
    }

    public byte[] downloadOfferLetter(Long requestId, java.security.Principal principal) throws IOException {
        if (principal == null || principal.getName() == null) {
            throw new com.placementos.internship.exception.AppException("Authentication required to access document", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        InternshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship request not found with id: " + requestId));

        User requester = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        boolean isStaff = requester.getRole() == Role.CDPC_STAFF || requester.getRole() == Role.CDPC_ADMIN;
        boolean isOwner = request.getStudent() != null
                && request.getStudent().getUser() != null
                && requester.getId().equals(request.getStudent().getUser().getId());

        if (!isStaff && !isOwner) {
            throw new com.placementos.internship.exception.AppException("Access denied: You do not have permission to download this offer letter", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (request.getOfferLetterUrl() == null) {
            throw new ResourceNotFoundException("No offer letter uploaded for this request");
        }

        Path path = Paths.get(request.getOfferLetterUrl());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Offer letter file not found on disk");
        }

        return Files.readAllBytes(path);
    }

    public byte[] downloadNocFile(Long requestId, java.security.Principal principal) throws IOException {
        if (principal == null || principal.getName() == null) {
            throw new com.placementos.internship.exception.AppException("Authentication required to access document", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        InternshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship request not found with id: " + requestId));

        User requester = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        boolean isStaff = requester.getRole() == Role.CDPC_STAFF || requester.getRole() == Role.CDPC_ADMIN;
        boolean isOwner = request.getStudent() != null
                && request.getStudent().getUser() != null
                && requester.getId().equals(request.getStudent().getUser().getId());

        if (!isStaff && !isOwner) {
            throw new com.placementos.internship.exception.AppException("Access denied: You do not have permission to download this NOC document", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        NocRecord noc = nocRecordRepository != null ? nocRecordRepository.findByRequestId(requestId).orElse(null) : null;
        String pdfPath = noc != null ? noc.getPdfPath() : "uploads/noc/NOC_Request_" + requestId + ".pdf";

        Path path = Paths.get(pdfPath);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("NOC document not found on disk");
        }

        return Files.readAllBytes(path);
    }

    private InternshipResponseDTO mapToResponseDTO(InternshipRequest request,
                                                  List<RuleEvaluationResultDTO> ruleResults,
                                                  List<ScamPatternFlag> scamFlags) {
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

        return InternshipResponseDTO.builder()
                .id(request.getId())
                .studentId(request.getStudent().getId())
                .studentName(request.getStudent().getUser() != null ? request.getStudent().getUser().getName() : null)
                .collegeId(request.getStudent().getCollegeId())
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
                .ruleEvaluations(ruleResults)
                .scamFlags(flagDTOs)
                .build();
    }
}
