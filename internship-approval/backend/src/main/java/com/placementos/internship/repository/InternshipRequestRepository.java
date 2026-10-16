package com.placementos.internship.repository;

import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.InternshipRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface InternshipRequestRepository extends JpaRepository<InternshipRequest, Long> {
    List<InternshipRequest> findByStudentId(Long studentId);
    List<InternshipRequest> findByStatus(InternshipRequestStatus status);
    List<InternshipRequest> findByStatusIn(Collection<InternshipRequestStatus> statuses);
    List<InternshipRequest> findByCompanyNameIgnoreCaseAndCreatedAtAfter(String companyName, LocalDateTime cutoffDate);
    List<InternshipRequest> findByHrContactEmailIgnoreCaseAndCreatedAtAfter(String hrContactEmail, LocalDateTime cutoffDate);
    List<InternshipRequest> findByOfferLetterHashAndCreatedAtAfter(String offerLetterHash, LocalDateTime cutoffDate);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM InternshipRequest r LEFT JOIN FETCH r.student s LEFT JOIN FETCH s.user WHERE r.id = :id")
    java.util.Optional<InternshipRequest> findByIdWithStudentAndUser(@org.springframework.data.repository.query.Param("id") Long id);
}

