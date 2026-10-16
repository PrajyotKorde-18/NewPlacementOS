package com.placementos.internship.repository;

import com.placementos.internship.entity.NocRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NocRecordRepository extends JpaRepository<NocRecord, Long> {
    Optional<NocRecord> findByRequestId(Long requestId);
    Optional<NocRecord> findByVerificationToken(String verificationToken);
    java.util.List<NocRecord> findByVerificationTokenIsNull();
}
