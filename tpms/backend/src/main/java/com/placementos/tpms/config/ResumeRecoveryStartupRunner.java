package com.placementos.tpms.config;

import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Startup recovery component that identifies any ResumeVersion records that were
 * left in PENDING or PROCESSING status (e.g. due to an abrupt server restart or crash)
 * and safely transitions them to FAILED with an explicit diagnostic explanation.
 */
@Component
@Order(50)
@RequiredArgsConstructor
@Slf4j
public class ResumeRecoveryStartupRunner implements ApplicationRunner {

    private final ResumeVersionRepository resumeVersionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[STARTUP RECOVERY] Checking for orphaned resume extraction jobs left in PENDING or PROCESSING state...");

        List<ResumeVersion> pendingResumes = resumeVersionRepository.findByExtractionStatus(ExtractionStatus.PENDING);
        List<ResumeVersion> processingResumes = resumeVersionRepository.findByExtractionStatus(ExtractionStatus.PROCESSING);

        int recoveredCount = 0;

        for (ResumeVersion rv : pendingResumes) {
            rv.setExtractionStatus(ExtractionStatus.FAILED);
            rv.setErrorLog("Interrupted by server restart or shutdown while in PENDING state.");
            resumeVersionRepository.save(rv);
            recoveredCount++;
            log.warn("[STARTUP RECOVERY] Marked orphaned ResumeVersion ID: {} (was PENDING) as FAILED due to server restart", rv.getId());
        }

        for (ResumeVersion rv : processingResumes) {
            rv.setExtractionStatus(ExtractionStatus.FAILED);
            rv.setErrorLog("Interrupted by server restart or shutdown while in PROCESSING state.");
            resumeVersionRepository.save(rv);
            recoveredCount++;
            log.warn("[STARTUP RECOVERY] Marked orphaned ResumeVersion ID: {} (was PROCESSING) as FAILED due to server restart", rv.getId());
        }

        if (recoveredCount > 0) {
            log.info("[STARTUP RECOVERY] Completed recovery: {} orphaned resume job(s) transitioned to FAILED.", recoveredCount);
        } else {
            log.info("[STARTUP RECOVERY] Completed recovery: No orphaned resume jobs found.");
        }
    }
}
