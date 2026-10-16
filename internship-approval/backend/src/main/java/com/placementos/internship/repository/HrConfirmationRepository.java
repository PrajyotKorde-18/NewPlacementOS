package com.placementos.internship.repository;

import com.placementos.internship.entity.HrConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrConfirmationRepository extends JpaRepository<HrConfirmation, Long> {
    Optional<HrConfirmation> findByRequestId(Long requestId);
    Optional<HrConfirmation> findByTokenHash(String tokenHash);
}
