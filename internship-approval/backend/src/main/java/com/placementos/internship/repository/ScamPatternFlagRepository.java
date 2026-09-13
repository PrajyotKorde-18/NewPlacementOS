package com.placementos.internship.repository;

import com.placementos.internship.entity.ScamPatternFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScamPatternFlagRepository extends JpaRepository<ScamPatternFlag, Long> {
    List<ScamPatternFlag> findByFlagType(String flagType);
    Optional<ScamPatternFlag> findFirstByFlagTypeAndMatchKeyAndDetectedAtAfter(
            String flagType, String matchKey, LocalDateTime cutoffDate
    );
}
