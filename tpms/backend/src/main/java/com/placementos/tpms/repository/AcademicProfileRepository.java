package com.placementos.tpms.repository;

import com.placementos.tpms.entity.AcademicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicProfileRepository extends JpaRepository<AcademicProfile, Long> {

    Optional<AcademicProfile> findByStudentId(Long studentId);

    @Query("""
        SELECT ap.studentId FROM AcademicProfile ap
        WHERE (:minCgpa IS NULL OR (
            SELECT COALESCE(AVG(s.sgpa), 0.0) FROM SemesterSgpa s 
            WHERE s.academicProfile.studentId = ap.studentId AND s.isFinalized = true
        ) >= :minCgpa)
        AND (:maxActiveBacklogs IS NULL OR (
            SELECT COUNT(b.id) FROM BacklogRecord b 
            WHERE b.academicProfile.studentId = ap.studentId AND b.cleared = false
        ) <= :maxActiveBacklogs)
    """)
    List<Long> findEligibleStudentIds(
            @Param("minCgpa") Double minCgpa,
            @Param("maxActiveBacklogs") Integer maxActiveBacklogs
    );
}
