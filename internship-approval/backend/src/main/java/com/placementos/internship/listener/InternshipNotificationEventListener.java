package com.placementos.internship.listener;

import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.NocRecord;
import com.placementos.internship.entity.Student;
import com.placementos.internship.event.InternshipApprovedEvent;
import com.placementos.internship.event.InternshipRejectedEvent;
import com.placementos.internship.event.InternshipSubmittedEvent;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.NocRecordRepository;
import com.placementos.internship.service.HrConfirmationService;
import com.placementos.internship.service.InternshipEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternshipNotificationEventListener {

    private final InternshipEmailService emailService;
    private final InternshipRequestRepository requestRepository;
    private final NocRecordRepository nocRecordRepository;
    private final HrConfirmationService hrConfirmationService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInternshipApproved(InternshipApprovedEvent event) {
        log.info("[INTERNSHIP NOTIFY] Handling InternshipApprovedEvent for request ID: {}", event.getRequestId());
        try {
            InternshipRequest request = requestRepository.findById(event.getRequestId()).orElse(null);
            if (request == null) return;

            NocRecord nocRecord = nocRecordRepository.findById(event.getNocRecordId()).orElse(null);

            Student student = request.getStudent();
            if (student != null && student.getUser() != null && student.getUser().getEmail() != null) {
                emailService.sendApprovedEmail(
                        student.getUser().getEmail(),
                        student.getUser().getName(),
                        request,
                        nocRecord
                );
            }
        } catch (Exception e) {
            log.error("[INTERNSHIP NOTIFY] Error processing InternshipApprovedEvent: {}", e.getMessage(), e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInternshipRejected(InternshipRejectedEvent event) {
        log.info("[INTERNSHIP NOTIFY] Handling InternshipRejectedEvent for request ID: {}", event.getRequestId());
        try {
            InternshipRequest request = requestRepository.findById(event.getRequestId()).orElse(null);
            if (request == null) return;

            Student student = request.getStudent();
            if (student != null && student.getUser() != null && student.getUser().getEmail() != null) {
                emailService.sendRejectedEmail(
                        student.getUser().getEmail(),
                        student.getUser().getName(),
                        request,
                        event.getRejectionReason()
                );
            }
        } catch (Exception e) {
            log.error("[INTERNSHIP NOTIFY] Error processing InternshipRejectedEvent: {}", e.getMessage(), e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInternshipSubmitted(InternshipSubmittedEvent event) {
        log.info("[INTERNSHIP NOTIFY] Handling InternshipSubmittedEvent for request ID: {}", event.getRequestId());
        try {
            hrConfirmationService.processInitialSubmission(event.getRequestId());
        } catch (Exception e) {
            log.error("[INTERNSHIP NOTIFY] Error processing InternshipSubmittedEvent for request #{}: {}", event.getRequestId(), e.getMessage(), e);
        }
    }
}
