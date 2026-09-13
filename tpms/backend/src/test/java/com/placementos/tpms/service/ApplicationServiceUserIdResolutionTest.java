package com.placementos.tpms.service;

import com.placementos.tpms.dto.ApplicationResponseDTO;
import com.placementos.tpms.entity.Application;
import com.placementos.tpms.entity.ApplicationStatus;
import com.placementos.tpms.entity.Drive;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.ApplicationRepository;
import com.placementos.tpms.repository.DriveRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
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
public class ApplicationServiceUserIdResolutionTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private DriveRepository driveRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void testGetApplicationsByStudent_WhenPassedUserId_ResolvesApplicationsCorrectly() {
        // 1. Create a coordinator user
        User coord = userRepository.save(User.builder()
                .email("coord_test@rknec.edu")
                .name("Test Coordinator")
                .password("pass")
                .role("COORDINATOR")
                .build());

        // 2. Create a Drive (e.g. NeuraCore)
        Drive drive = driveRepository.save(Drive.builder()
                .collegeId("RKNEC")
                .companyName("NeuraCore")
                .roleTitle("Machine Learning Engineer")
                .vacancies(1)
                .ctc("12 LPA")
                .createdBy(coord)
                .build());

        // 3. Create a student User (Akash Verma)
        User studentUser = userRepository.save(User.builder()
                .email("akash.verma.test@rknec.edu")
                .name("Akash Verma")
                .password("pass")
                .role("STUDENT")
                .build());

        // 4. Create Student entity linked to Akash Verma
        Student student = studentRepository.save(Student.builder()
                .user(studentUser)
                .collegeId("AKASH_TEST_001")
                .build());

        // 5. Save an Application linked to student
        Application application = applicationRepository.save(Application.builder()
                .drive(drive)
                .student(student)
                .status(ApplicationStatus.PENDING)
                .build());

        // 6. Query applications using studentUser.getId() (User ID passed from frontend)
        List<ApplicationResponseDTO> appsByUser = applicationService.getApplicationsByStudent(studentUser.getId());

        assertNotNull(appsByUser);
        assertEquals(1, appsByUser.size(), "Should retrieve 1 application when queried with User ID");
        assertEquals("NeuraCore", appsByUser.get(0).getCompanyName());
        assertEquals(application.getId(), appsByUser.get(0).getId());

        // 7. Query applications using student.getId() (Student entity ID)
        List<ApplicationResponseDTO> appsByStudentId = applicationService.getApplicationsByStudent(student.getId());

        assertNotNull(appsByStudentId);
        assertEquals(1, appsByStudentId.size(), "Should retrieve 1 application when queried with Student entity ID");
        assertEquals("NeuraCore", appsByStudentId.get(0).getCompanyName());
    }
}
