package com.placementos.tpms.service;

import com.placementos.tpms.dto.ResumeUploadResponse;
import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.ResumeVersionRepository;
import com.placementos.tpms.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private com.placementos.tpms.repository.UserRepository userRepository;

    @Mock
    private AsyncResumeProcessor asyncResumeProcessor;

    @InjectMocks
    private ResumeService resumeService;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        User testUser = User.builder().id(100L).email("student@rknec.edu").name("Jane Doe").role("STUDENT").build();
        testStudent = Student.builder().id(10L).user(testUser).collegeId("CS2026001").build();
    }

    @Test
    void testUploadResume_ReturnsPendingImmediatelyAndEnqueuesAsyncProcessing() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(testStudent));
        when(resumeVersionRepository.saveAndFlush(any(ResumeVersion.class))).thenAnswer(invocation -> {
            ResumeVersion rv = invocation.getArgument(0);
            if (rv.getId() == null) rv.setId(1L);
            return rv;
        });
        when(resumeVersionRepository.findById(1L)).thenReturn(Optional.of(
                ResumeVersion.builder().id(1L).student(testStudent).extractionStatus(ExtractionStatus.PENDING).build()
        ));

        MockMultipartFile mockFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", "dummy resume content".getBytes());

        ResumeUploadResponse response = resumeService.uploadResume(10L, mockFile);

        assertNotNull(response);
        assertEquals(ExtractionStatus.PENDING, response.getExtractionStatus());
        assertEquals(10L, response.getStudentId());
        assertEquals(1L, response.getResumeId());

        // Verify sync processor trigger
        verify(asyncResumeProcessor, times(1)).processResumeSync(1L);
    }
}
