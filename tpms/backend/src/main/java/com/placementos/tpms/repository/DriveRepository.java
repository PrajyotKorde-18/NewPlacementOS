package com.placementos.tpms.repository;

import com.placementos.tpms.entity.Drive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriveRepository extends JpaRepository<Drive, Long> {

    List<Drive> findByCollegeId(String collegeId);

    List<Drive> findByCreatedById(Long createdById);
}
