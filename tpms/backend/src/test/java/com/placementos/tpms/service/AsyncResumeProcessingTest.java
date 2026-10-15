package com.placementos.tpms.service;

import com.placementos.tpms.config.ResumeRecoveryStartupRunner;
import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.ResumeUploadResponse;
import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.StudentProfile;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.exception.AppException;
import com.placementos.tpms.repository.ResumeVersionRepository;
import com.placementos.tpms.repository.StudentProfileRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AsyncResumeProcessingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private AsyncResumeProcessor asyncResumeProcessor;

    @Autowired
    private ResumeRecoveryStartupRunner resumeRecoveryStartupRunner;

    @Autowired
    private ResumeVersionRepository resumeVersionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ResumeExtractionService resumeExtractionService;

    private Student testStudent;
    private MockMultipartFile validPdfFile;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure test upload folder exists
        File uploadFolder = new File("uploads/resumes");
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        User user = userRepository.findByEmail("async.student@rknec.edu")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Async Candidate")
                        .email("async.student@rknec.edu")
                        .password("hashed_pwd")
                        .role("STUDENT")
                        .build()));

        testStudent = studentRepository.findByUserId(user.getId())
                .orElseGet(() -> studentRepository.save(Student.builder()
                        .user(user)
                        .collegeId("COLL_ASYNC_01")
                        .build()));

        byte[] dummyPdf = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF".getBytes(StandardCharsets.UTF_8);

        validPdfFile = new MockMultipartFile(
                "file",
                "sample_resume.pdf",
                "application/pdf",
                dummyPdf
        );
    }

    @Test
    @DisplayName("1. Upload returns HTTP 202 before async extraction finishes")
    @org.springframework.security.test.context.support.WithMockUser(username = "async.student@rknec.edu", roles = {"STUDENT"})
    void testUploadReturnsBeforeExtractionFinishes() throws Exception {
        CountDownLatch extractionStartedLatch = new CountDownLatch(1);
        CountDownLatch allowExtractionToFinishLatch = new CountDownLatch(1);

        // Simulate long-running LLM extraction that blocks on allowExtractionToFinishLatch
        when(resumeExtractionService.extractStructuredData(any())).thenAnswer(invocation -> {
            extractionStartedLatch.countDown();
            // Block async thread until released or test times out
            allowExtractionToFinishLatch.await(5, TimeUnit.SECONDS);
            return ParsedResumeDTO.builder()
                    .skills(List.of("Java", "Spring Boot", "Docker"))
                    .projects(List.of(java.util.Map.of("title", "Placement Portal")))
                    .extractionSource("LLM")
                    .build();
        });

        // Execute upload via MockMvc
        mockMvc.perform(multipart("/resumes/upload")
                        .file(validPdfFile)
                        .param("studentId", testStudent.getId().toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.versionId", notNullValue()))
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));

        // Confirm extraction started in the background but latch hasn't been allowed to finish yet
        boolean started = extractionStartedLatch.await(3, TimeUnit.SECONDS);
        assertTrue(started, "Async extraction worker should have picked up the job in the background");

        // Release latch to let async thread complete cleanly
        allowExtractionToFinishLatch.countDown();
    }

    @Test
    @DisplayName("2. PENDING to COMPLETED: Successful async extraction updates status and profile")
    void testPendingToCompletedTransition() throws Exception {
        CountDownLatch finishedLatch = new CountDownLatch(1);

        when(resumeExtractionService.extractStructuredData(any())).thenAnswer(invocation -> {
            try {
                return ParsedResumeDTO.builder()
                        .skills(List.of("React", "TypeScript", "Node.js"))
                        .projects(List.of(java.util.Map.of("title", "E-Commerce Microservices")))
                        .internships(List.of(java.util.Map.of("company", "Acme", "role", "Intern")))
                        .extractionSource("LLM")
                        .build();
            } finally {
                finishedLatch.countDown();
            }
        });

        // Upload returns PENDING immediately and enqueues async processing
        ResumeUploadResponse response = resumeService.uploadResume(testStudent.getId(), validPdfFile);
        assertNotNull(response);
        assertEquals(ExtractionStatus.PENDING, response.getExtractionStatus());
        Long versionId = response.getVersionId();

        // Wait for background async worker to finish
        assertTrue(finishedLatch.await(5, TimeUnit.SECONDS), "Async processing should finish");
        Thread.sleep(150); // allow transaction commit to complete

        // Assert DB status transitioned to COMPLETED
        ResumeVersion updated = resumeVersionRepository.findById(versionId).orElseThrow();
        assertEquals(ExtractionStatus.COMPLETED, updated.getExtractionStatus());
        assertNull(updated.getErrorLog());

        // Assert student profile was updated
        StudentProfile profile = studentProfileRepository.findByStudentId(testStudent.getId()).orElseThrow();
        assertNotNull(profile.getSkills());
        assertTrue(profile.getSkills().contains("React"));
        assertTrue(profile.getSkills().contains("TypeScript"));
    }

    @Test
    @DisplayName("3. Failure ends in FAILED: Exception in extraction marks status FAILED with errorLog")
    void testFailureEndsInFailed() throws Exception {
        CountDownLatch failedLatch = new CountDownLatch(1);

        when(resumeExtractionService.extractStructuredData(any())).thenAnswer(invocation -> {
            failedLatch.countDown();
            throw new RuntimeException("Groq API quota exhausted or model unavailable");
        });

        ResumeUploadResponse response = resumeService.uploadResume(testStudent.getId(), validPdfFile);
        Long versionId = response.getVersionId();

        // Wait for background async worker to execute failure path
        assertTrue(failedLatch.await(5, TimeUnit.SECONDS), "Async processing should reach extraction call");
        Thread.sleep(150); // allow catch block to commit FAILED status

        // Assert DB status transitioned to FAILED with errorLog
        ResumeVersion updated = resumeVersionRepository.findById(versionId).orElseThrow();
        assertEquals(ExtractionStatus.FAILED, updated.getExtractionStatus());
        assertNotNull(updated.getErrorLog());
        assertTrue(updated.getErrorLog().contains("Groq API quota exhausted"));
    }

    @Test
    @DisplayName("4. Startup recovery: Mark orphaned PENDING and PROCESSING as FAILED with clear reason")
    void testStartupRecovery() {
        // Create an orphaned PENDING resume
        ResumeVersion pendingResume = resumeVersionRepository.save(ResumeVersion.builder()
                .student(testStudent)
                .filePath("uploads/resumes/orphaned_pending.pdf")
                .extractionStatus(ExtractionStatus.PENDING)
                .build());

        // Create an orphaned PROCESSING resume
        ResumeVersion processingResume = resumeVersionRepository.save(ResumeVersion.builder()
                .student(testStudent)
                .filePath("uploads/resumes/orphaned_processing.pdf")
                .extractionStatus(ExtractionStatus.PROCESSING)
                .build());

        // Create a legitimately COMPLETED resume
        ResumeVersion completedResume = resumeVersionRepository.save(ResumeVersion.builder()
                .student(testStudent)
                .filePath("uploads/resumes/completed.pdf")
                .extractionStatus(ExtractionStatus.COMPLETED)
                .build());

        // Trigger startup recovery runner
        resumeRecoveryStartupRunner.run(null);

        // Verify orphaned PENDING -> FAILED
        ResumeVersion recoveredPending = resumeVersionRepository.findById(pendingResume.getId()).orElseThrow();
        assertEquals(ExtractionStatus.FAILED, recoveredPending.getExtractionStatus());
        assertEquals("Interrupted by server restart or shutdown while in PENDING state.", recoveredPending.getErrorLog());

        // Verify orphaned PROCESSING -> FAILED
        ResumeVersion recoveredProcessing = resumeVersionRepository.findById(processingResume.getId()).orElseThrow();
        assertEquals(ExtractionStatus.FAILED, recoveredProcessing.getExtractionStatus());
        assertEquals("Interrupted by server restart or shutdown while in PROCESSING state.", recoveredProcessing.getErrorLog());

        // Verify COMPLETED remains untouched
        ResumeVersion unchangedCompleted = resumeVersionRepository.findById(completedResume.getId()).orElseThrow();
        assertEquals(ExtractionStatus.COMPLETED, unchangedCompleted.getExtractionStatus());
    }
}
