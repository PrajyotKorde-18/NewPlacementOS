package com.placementos.internship.repository;

import com.placementos.internship.entity.RuleEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleEvaluationRepository extends JpaRepository<RuleEvaluation, Long> {
    List<RuleEvaluation> findByRequestId(Long requestId);
}
