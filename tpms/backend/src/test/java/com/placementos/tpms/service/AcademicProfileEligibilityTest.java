package com.placementos.tpms.service;

import com.placementos.tpms.entity.AcademicProfile;
import com.placementos.tpms.entity.BacklogRecord;
import com.placementos.tpms.entity.SemesterSgpa;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.AcademicProfileRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AcademicProfileEligibilityTest {

    @Autowired
    private AcademicProfileRepository academicProfileRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private Student studentMulti;
    private Student studentZeroBacklog;
    private Student studentNoSem;

    @BeforeEach
    void setUp() {
        // Student 1: 8 finalized semesters (SGPAs: 8.0, 8.5, 7.5, 9.0, 8.0, 8.5, 9.0, 9.0 -> Sum = 67.5, AVG = 8.4375 -> rounded 8.44) and 2 active backlogs
        User user1 = userRepository.save(User.builder().email("elig_student1@rknec.edu").name("Student One").password("pass").build());
        studentMulti = studentRepository.save(Student.builder().user(user1).collegeId("COLL001").build());

        AcademicProfile ap1 = AcademicProfile.builder().student(studentMulti).studentId(studentMulti.getId()).build();
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(1).sgpa(8.0).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(2).sgpa(8.5).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(3).sgpa(7.5).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(4).sgpa(9.0).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(5).sgpa(8.0).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(6).sgpa(8.5).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(7).sgpa(9.0).isFinalized(true).build());
        ap1.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap1).semesterNumber(8).sgpa(9.0).isFinalized(true).build());

        ap1.getBacklogRecords().add(BacklogRecord.builder().academicProfile(ap1).subjectName("Maths 1").semester(1).cleared(false).build());
        ap1.getBacklogRecords().add(BacklogRecord.builder().academicProfile(ap1).subjectName("Physics").semester(2).cleared(false).build());
        ap1.getBacklogRecords().add(BacklogRecord.builder().academicProfile(ap1).subjectName("Chemistry").semester(1).cleared(true).build()); // Cleared backlog
        academicProfileRepository.save(ap1);

        // Student 2: 4 finalized semesters (SGPAs: 9.0, 9.0, 9.0, 9.0 -> AVG = 9.0) and 0 active backlogs
        User user2 = userRepository.save(User.builder().email("elig_student2@rknec.edu").name("Student Two").password("pass").build());
        studentZeroBacklog = studentRepository.save(Student.builder().user(user2).collegeId("COLL002").build());

        AcademicProfile ap2 = AcademicProfile.builder().student(studentZeroBacklog).studentId(studentZeroBacklog.getId()).build();
        for (int i = 1; i <= 4; i++) {
            ap2.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap2).semesterNumber(i).sgpa(9.0).isFinalized(true).build());
        }
        academicProfileRepository.save(ap2);

        // Student 3: No finalized semesters, no backlog records
        User user3 = userRepository.save(User.builder().email("elig_student3@rknec.edu").name("Student Three").password("pass").build());
        studentNoSem = studentRepository.save(Student.builder().user(user3).collegeId("COLL003").build());
        AcademicProfile ap3 = AcademicProfile.builder().student(studentNoSem).studentId(studentNoSem.getId()).build();
        academicProfileRepository.save(ap3);
    }

    @Test
    void testJavaEntityDerivations_NoCartesianMultiplier() {
        AcademicProfile ap = academicProfileRepository.findByStudentId(studentMulti.getId()).orElseThrow();

        // 8 finalized semesters: Sum = 67.5 / 8 = 8.4375 -> rounded 8.44
        assertEquals(8.44, ap.getCurrentCgpa());
        assertEquals(2L, ap.getActiveBacklogCount(), "Active backlog count must be 2, not multiplied by 8 finalized semesters!");
        assertEquals(3L, ap.getTotalEverBacklogCount());
    }

    @Test
    void testDbEligibilityQuery_NoCartesianProductMultiplication() {
        // Query for min CGPA 8.0 and max active backlogs 2
        List<Long> eligible = academicProfileRepository.findEligibleStudentIds(8.0, 2);

        assertTrue(eligible.contains(studentMulti.getId()), "Student 1 must be eligible for min CGPA 8.0 and max 2 backlogs");
        assertTrue(eligible.contains(studentZeroBacklog.getId()));

        // Query for max active backlogs 1 -> Student 1 has 2 active backlogs, so must NOT be included.
        // If Cartesian product were present, count would be 16! Here count is correctly 2.
        List<Long> maxOneBacklog = academicProfileRepository.findEligibleStudentIds(8.0, 1);
        assertFalse(maxOneBacklog.contains(studentMulti.getId()), "Student 1 with 2 backlogs must be excluded when max backlogs is 1");
        assertTrue(maxOneBacklog.contains(studentZeroBacklog.getId()));

        // Query for min CGPA 8.5 -> Student 1 has average 8.4375, so must NOT be included
        List<Long> minCgpa85 = academicProfileRepository.findEligibleStudentIds(8.5, 2);
        assertFalse(minCgpa85.contains(studentMulti.getId()), "Student 1 with CGPA 8.4375 must be excluded when min CGPA is 8.5");
        assertTrue(minCgpa85.contains(studentZeroBacklog.getId()));
    }
}
