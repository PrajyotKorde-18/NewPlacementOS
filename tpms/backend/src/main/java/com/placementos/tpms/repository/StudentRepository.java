package com.placementos.tpms.repository;

import com.placementos.tpms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByCollegeId(String collegeId);

    Optional<Student> findByCollegeRollNumber(String collegeRollNumber);

    Optional<Student> findByUserEmail(String email);

    boolean existsByCollegeId(String collegeId);

    boolean existsByCollegeRollNumber(String collegeRollNumber);
}
