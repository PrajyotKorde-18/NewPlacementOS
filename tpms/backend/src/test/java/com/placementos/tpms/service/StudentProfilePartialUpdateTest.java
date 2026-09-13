package com.placementos.tpms.service;

import com.placementos.tpms.dto.*;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudentProfilePartialUpdateTest {

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AcademicProfileRepository academicProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder().email("partial_update_student@rknec.edu").name("Partial Student").password("pass").build());
        testStudent = studentRepository.save(Student.builder().user(user).collegeId("PARTIAL001").phoneNumber("1111111111").build());

        // Create profile with existing certifications, SGPAs, and backlogs
        StudentProfile profile = studentProfileRepository.save(StudentProfile.builder().student(testStudent).studentId(testStudent.getId()).build());
        profile.getStructuredCertifications().add(StudentCertification.builder().studentProfile(profile).name("AWS Certified").issuingBody("Amazon").build());
        profile.getStructuredCertifications().add(StudentCertification.builder().studentProfile(profile).name("OCJP").issuingBody("Oracle").build());
        studentProfileRepository.save(profile);

        AcademicProfile ap = academicProfileRepository.save(AcademicProfile.builder().student(testStudent).studentId(testStudent.getId()).build());
        ap.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap).semesterNumber(1).sgpa(8.0).isFinalized(true).build());
        ap.getSemesterSgpas().add(SemesterSgpa.builder().academicProfile(ap).semesterNumber(2).sgpa(8.5).isFinalized(true).build());
        ap.getBacklogRecords().add(BacklogRecord.builder().academicProfile(ap).subjectName("Data Structures").semester(2).cleared(false).build());
        academicProfileRepository.save(ap);
    }

    @Test
    void testPartialUpdate_PhoneOnlyPUT_DoesNotWipeNestedRecords() {
        // Send a PUT with ONLY phoneNumber specified (structuredCertifications, semesterSgpas, backlogRecords are null)
        StudentProfileUpdateDTO updateDTO = StudentProfileUpdateDTO.builder()
                .phoneNumber("9999999999")
                .build();

        StudentFullProfileDTO result = studentProfileService.updateStudentProfile(testStudent.getId(), updateDTO);

        assertEquals("9999999999", result.getPhoneNumber());

        // Assert nested history survived intact!
        assertEquals(2, result.getCertifications().size(), "Certifications must not be deleted on phone-only partial PUT!");
        assertNotNull(result.getAcademicProfile());
        assertEquals(2, result.getAcademicProfile().getSemesterSgpas().size(), "Semester SGPAs must survive phone-only partial PUT!");
        assertEquals(1, result.getAcademicProfile().getBacklogRecords().size(), "Backlog records must survive phone-only partial PUT!");

        // Reload directly from DB to verify persistence state
        StudentProfile dbProfile = studentProfileRepository.findByStudentId(testStudent.getId()).orElseThrow();
        assertEquals(2, dbProfile.getStructuredCertifications().size());

        AcademicProfile dbAcademic = academicProfileRepository.findByStudentId(testStudent.getId()).orElseThrow();
        assertEquals(2, dbAcademic.getSemesterSgpas().size());
        assertEquals(1, dbAcademic.getBacklogRecords().size());
    }

    @Test
    void testPartialUpdate_ExplicitEmptyList_ClearsOnlyTargetCollection() {
        // Send a PUT with explicitly empty structuredCertifications list
        StudentProfileUpdateDTO updateDTO = StudentProfileUpdateDTO.builder()
                .structuredCertifications(Collections.emptyList())
                .build();

        StudentFullProfileDTO result = studentProfileService.updateStudentProfile(testStudent.getId(), updateDTO);

        assertEquals(0, result.getCertifications().size(), "Explicit [] payload must clear structuredCertifications");
        assertEquals(2, result.getAcademicProfile().getSemesterSgpas().size(), "Semester SGPAs must remain untouched");
        assertEquals(1, result.getAcademicProfile().getBacklogRecords().size(), "Backlog records must remain untouched");
    }
}
