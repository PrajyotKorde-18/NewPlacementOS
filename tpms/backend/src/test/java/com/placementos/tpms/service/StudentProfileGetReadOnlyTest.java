package com.placementos.tpms.service;

import com.placementos.tpms.dto.StudentFullProfileDTO;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudentProfileGetReadOnlyTest {

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testGetStudentProfile_WhenStudentDoesNotExist_DoesNotInsertRecord() {
        // Create a User record without an associated Student entity
        User user = userRepository.save(User.builder().email("orphan_user@rknec.edu").name("Orphan User").password("pass").build());
        Long orphanUserId = user.getId();

        long initialStudentCount = studentRepository.count();

        // Attempting GET for orphan user must throw ResourceNotFoundException (404)
        assertThrows(ResourceNotFoundException.class, () -> {
            studentProfileService.getStudentProfile(orphanUserId);
        });

        long finalStudentCount = studentRepository.count();

        // Assert zero INSERTs occurred during GET request!
        assertEquals(initialStudentCount, finalStudentCount, "GET profile request must be strictly read-only and perform zero INSERTs!");
    }

    @Test
    void testGetStudentProfile_WhenStudentExists_ReturnsProfileWithoutMutatingDatabase() {
        User user = userRepository.save(User.builder().email("existing_student@rknec.edu").name("Existing Student").password("pass").build());
        Student student = studentRepository.save(Student.builder().user(user).collegeId("EXISTING001").build());

        long initialStudentCount = studentRepository.count();

        StudentFullProfileDTO dto = studentProfileService.getStudentProfile(student.getId());

        assertNotNull(dto);
        assertEquals(student.getId(), dto.getStudentId());
        assertEquals("EXISTING001", dto.getCollegeId());

        long finalStudentCount = studentRepository.count();
        assertEquals(initialStudentCount, finalStudentCount, "GET request for existing student must be read-only!");
    }
}
