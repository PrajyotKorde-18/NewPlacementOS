package com.placementos.tpms.listener;

import com.placementos.tpms.entity.*;
import com.placementos.tpms.event.*;
import com.placementos.tpms.repository.*;
import com.placementos.tpms.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class TpmsNotificationEventListener {

    private final EmailNotificationService emailService;
    private final DriveRepository driveRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final StudentRepository studentRepository;
    private final AcademicProfileRepository academicProfileRepository;
    private final ApplicationRepository applicationRepository;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrivePublished(DrivePublishedEvent event) {
        log.info("[TPMS NOTIFY] Handling DrivePublishedEvent for drive ID: {}", event.getDriveId());
        try {
            Drive drive = driveRepository.findById(event.getDriveId()).orElse(null);
            if (drive == null) return;

            RoleProfile roleProfile = roleProfileRepository.findByDriveId(drive.getId()).orElse(null);

            Set<Long> eligibleStudentIds = new HashSet<>();
            if (roleProfile != null && (roleProfile.getMinCgpa() != null || roleProfile.getMaxActiveBacklogs() != null)) {
                List<Long> matchedIds = academicProfileRepository.findEligibleStudentIds(
                        roleProfile.getMinCgpa(),
                        roleProfile.getMaxActiveBacklogs()
                );
                eligibleStudentIds.addAll(matchedIds);
            } else {
                // If no CGPA or backlog criteria specified, consider all students
                studentRepository.findAll().forEach(s -> eligibleStudentIds.add(s.getId()));
            }

            // Further filter by 10th and 12th percentage if defined in roleProfile
            List<Student> finalEligibleStudents = new ArrayList<>();
            for (Long studentId : eligibleStudentIds) {
                Student student = studentRepository.findById(studentId).orElse(null);
                if (student == null) continue;

                AcademicProfile academic = academicProfileRepository.findByStudentId(studentId).orElse(null);
                if (roleProfile != null) {
                    if (roleProfile.getMinTenthPercentage() != null && roleProfile.getMinTenthPercentage() > 0) {
                        if (academic == null || academic.getTenthPercentage() == null || academic.getTenthPercentage() < roleProfile.getMinTenthPercentage()) {
                            continue;
                        }
                    }
                    if (roleProfile.getMinTwelfthPercentage() != null && roleProfile.getMinTwelfthPercentage() > 0) {
                        if (academic == null || academic.getTwelfthPercentage() == null || academic.getTwelfthPercentage() < roleProfile.getMinTwelfthPercentage()) {
                            continue;
                        }
                    }
                }
                finalEligibleStudents.add(student);
            }

            log.info("[TPMS NOTIFY] Found {} academically eligible students for drive '{}' ({})",
                    finalEligibleStudents.size(), drive.getCompanyName(), drive.getRoleTitle());

            for (Student student : finalEligibleStudents) {
                if (student.getUser() != null && student.getUser().getEmail() != null) {
                    emailService.sendDrivePublishedEmail(
                            student.getUser().getEmail(),
                            student.getUser().getName(),
                            drive,
                            roleProfile
                    );
                }
            }
        } catch (Exception e) {
            log.error("[TPMS NOTIFY] Error processing DrivePublishedEvent for drive ID {}: {}", event.getDriveId(), e.getMessage(), e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudentShortlisted(StudentShortlistedEvent event) {
        log.info("[TPMS NOTIFY] Handling StudentShortlistedEvent for application ID: {}", event.getApplicationId());
        try {
            Application app = applicationRepository.findById(event.getApplicationId()).orElse(null);
            if (app == null) return;

            Drive drive = app.getDrive();
            Student student = app.getStudent();
            if (student != null && student.getUser() != null && student.getUser().getEmail() != null) {
                // Slot booking link if slots exist in future / application link
                String slotBookingUrl = null;
                emailService.sendStudentShortlistedEmail(
                        student.getUser().getEmail(),
                        student.getUser().getName(),
                        drive,
                        app,
                        slotBookingUrl
                );
            }
        } catch (Exception e) {
            log.error("[TPMS NOTIFY] Error processing StudentShortlistedEvent: {}", e.getMessage(), e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRejectionFeedbackAvailable(RejectionFeedbackAvailableEvent event) {
        log.info("[TPMS NOTIFY] Handling RejectionFeedbackAvailableEvent for application ID: {}", event.getApplicationId());
        try {
            Application app = applicationRepository.findById(event.getApplicationId()).orElse(null);
            if (app == null) return;

            Drive drive = app.getDrive();
            Student student = app.getStudent();
            if (student != null && student.getUser() != null && student.getUser().getEmail() != null) {
                emailService.sendRejectionFeedbackAvailableEmail(
                        student.getUser().getEmail(),
                        student.getUser().getName(),
                        drive,
                        app
                );
            }
        } catch (Exception e) {
            log.error("[TPMS NOTIFY] Error processing RejectionFeedbackAvailableEvent: {}", e.getMessage(), e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterviewBooked(InterviewBookedEvent event) {
        log.info("[TPMS NOTIFY] Handling InterviewBookedEvent for application ID: {}", event.getApplicationId());
        try {
            Application app = applicationRepository.findById(event.getApplicationId()).orElse(null);
            if (app == null) return;

            Drive drive = app.getDrive();
            Student student = app.getStudent();
            if (student != null && student.getUser() != null && student.getUser().getEmail() != null) {
                emailService.sendInterviewBookedEmail(
                        student.getUser().getEmail(),
                        student.getUser().getName(),
                        drive,
                        event.getSlotDetails()
                );
            }
        } catch (Exception e) {
            log.error("[TPMS NOTIFY] Error processing InterviewBookedEvent: {}", e.getMessage(), e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterviewCancelled(InterviewCancelledEvent event) {
        log.info("[TPMS NOTIFY] Handling InterviewCancelledEvent for application ID: {}", event.getApplicationId());
        try {
            Application app = applicationRepository.findById(event.getApplicationId()).orElse(null);
            if (app == null) return;

            Drive drive = app.getDrive();
            Student student = app.getStudent();
            if (student != null && student.getUser() != null && student.getUser().getEmail() != null) {
                emailService.sendInterviewCancelledEmail(
                        student.getUser().getEmail(),
                        student.getUser().getName(),
                        drive,
                        event.getReason()
                );
            }
        } catch (Exception e) {
            log.error("[TPMS NOTIFY] Error processing InterviewCancelledEvent: {}", e.getMessage(), e);
        }
    }
}
