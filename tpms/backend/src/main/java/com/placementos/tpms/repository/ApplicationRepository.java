package com.placementos.tpms.repository;

import com.placementos.tpms.entity.Application;
import com.placementos.tpms.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByDriveIdAndStudentId(Long driveId, Long studentId);

    List<Application> findByDriveIdOrderByScoreDesc(Long driveId);

    List<Application> findByDriveIdAndStatus(Long driveId, ApplicationStatus status);

    List<Application> findByDriveIdAndStatusIn(Long driveId, Collection<ApplicationStatus> statuses);

    List<Application> findByStudentId(Long studentId);

    boolean existsByDriveIdAndStudentId(Long driveId, Long studentId);
}
