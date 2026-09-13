package com.placementos.tpms.repository;

import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    List<ResumeVersion> findByStudentIdOrderByUploadedAtDesc(Long studentId);

    Optional<ResumeVersion> findTopByStudentIdOrderByUploadedAtDesc(Long studentId);

    List<ResumeVersion> findByExtractionStatus(ExtractionStatus status);
}
