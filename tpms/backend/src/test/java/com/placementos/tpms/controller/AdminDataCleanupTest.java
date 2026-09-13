package com.placementos.tpms.controller;

import com.placementos.tpms.entity.Drive;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.DriveRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminDataCleanupTest {

    @Autowired
    private AdminDataCleanupController adminDataCleanupController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DriveRepository driveRepository;

    @Test
    void testCleanupAllData_PurgesStudentsAndCompanyDrives() {
        // Seed sample student & company drive
        User studentUser = userRepository.save(User.builder()
                .email("purge.student@rknec.edu")
                .name("Purge Student")
                .password("pass")
                .role("STUDENT")
                .build());

        studentRepository.save(Student.builder()
                .user(studentUser)
                .collegeId("PCOLL001")
                .collegeRollNumber("PROLL001")
                .build());

        driveRepository.save(Drive.builder()
                .companyName("Purge Corp")
                .roleTitle("Software Developer")
                .collegeId("PCOLL001")
                .createdBy(studentUser)
                .build());

        // Perform cleanup
        adminDataCleanupController.cleanupAllData();

        // Verify that student entries and company drive entries are completely deleted
        assertEquals(0, studentRepository.count(), "All student records must be deleted");
        assertEquals(0, driveRepository.count(), "All company placement drives must be deleted");
        assertEquals(0, userRepository.findByRole("STUDENT").size(), "All student user accounts must be deleted");
    }
}
