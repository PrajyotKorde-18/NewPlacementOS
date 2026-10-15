package com.placementos.tpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.config.DemoAccountProperties;
import com.placementos.tpms.dto.*;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.repository.ApplicationRepository;
import com.placementos.tpms.repository.DriveRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@Transactional
public class FrontendFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DemoAccountProperties demoProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DriveRepository driveRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private java.util.Optional<com.placementos.tpms.config.DemoAccountSeeder> demoAccountSeeder;

    private User demoStudentUser;
    private Student demoStudent;
    private Drive testDrive;

    @BeforeEach
    void setUp() {
        demoStudentUser = userRepository.findByEmail("candidate.student@rknec.edu").orElse(null);
        if (demoStudentUser == null) {
            demoAccountSeeder.ifPresent(com.placementos.tpms.config.DemoAccountSeeder::seedDemoAccounts);
            demoStudentUser = userRepository.findByEmail("candidate.student@rknec.edu").orElse(null);
        }
        if (demoStudentUser != null) {
            demoStudent = studentRepository.findByUserId(demoStudentUser.getId()).orElse(null);
        }

        User adminUser = userRepository.findByEmail("admin@rknec.edu").orElseGet(() ->
                userRepository.save(User.builder()
                        .name("College Admin")
                        .email("admin@rknec.edu")
                        .password("encoded_pass")
                        .role("ADMIN")
                        .build())
        );

        testDrive = driveRepository.save(Drive.builder()
                .collegeId("RKNECO1")
                .companyName("Amazon Web Services")
                .roleTitle("Cloud Support Associate")
                .vacancies(5)
                .ctc("14 LPA")
                .createdBy(adminUser)
                .build());
    }

    @Test
    @DisplayName("Frontend Flow: JSON Login Request matches AuthController specification")
    void testAuthLogin_JsonRequestAsFrontend() throws Exception {
        DemoAccountProperties.DemoAccountConfig studentConfig = demoProperties.getAccounts().stream()
                .filter(a -> "candidate.student@rknec.edu".equalsIgnoreCase(a.getEmail()))
                .findFirst()
                .orElseThrow();

        Map<String, String> loginPayload = Map.of(
                "email", studentConfig.getEmail(),
                "password", studentConfig.getPassword()
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", equalToIgnoringCase(studentConfig.getEmail())))
                .andExpect(jsonPath("$.role", equalTo("STUDENT")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @DisplayName("Frontend Flow: Multipart Resume Upload exactly as api.uploadResume sends it")
    @WithMockUser(username = "candidate.student@rknec.edu", roles = {"STUDENT"})
    void testResumeUpload_MultipartRequestAsFrontend() throws Exception {
        byte[] pdfBytes = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF".getBytes();
        MockMultipartFile resumeFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes);

        Long studentId = demoStudent != null ? demoStudent.getId() : 1L;

        mockMvc.perform(multipart("/resumes/upload")
                        .file(resumeFile)
                        .param("studentId", studentId.toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.versionId", notNullValue()))
                .andExpect(jsonPath("$.extractionStatus", notNullValue()));
    }

    @Test
    @DisplayName("Frontend Flow: Multipart JD File Extraction exactly as api.extractJdFile sends it")
    @WithMockUser(username = "admin@rknec.edu", roles = {"COORDINATOR"})
    void testExtractJdFile_MultipartRequestAsFrontend() throws Exception {
        byte[] pdfBytes = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000052 00000 n \n0000000101 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF".getBytes();
        MockMultipartFile jdFile = new MockMultipartFile("file", "jd.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart("/drives/extract-jd-file")
                        .file(jdFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredSkills", notNullValue()))
                .andExpect(jsonPath("$.weights", notNullValue()));
    }

    @Test
    @DisplayName("Frontend Flow: JSON Drive Creation exactly as api.createDrive sends it")
    @WithMockUser(username = "admin@rknec.edu", roles = {"COORDINATOR"})
    void testCreateDrive_JsonRequestAsFrontend() throws Exception {
        Map<String, Object> drivePayload = Map.of(
                "collegeId", "RKNECO1",
                "companyName", "Microsoft India",
                "roleTitle", "Software Engineer",
                "vacancies", 10,
                "ctc", "22 LPA",
                "roleProfile", Map.of(
                        "requiredSkills", Map.of("MUST_HAVE", List.of("Java", "Spring Boot")),
                        "weights", Map.of("skillMatch", 0.4, "projectRelevance", 0.3, "codingActivity", 0.1, "certifications", 0.1, "experience", 0.1)
                )
        );

        mockMvc.perform(post("/drives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(drivePayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.companyName", equalTo("Microsoft India")))
                .andExpect(jsonPath("$.vacancies", equalTo(10)));
    }

    @Test
    @DisplayName("Frontend Flow: JSON Student Profile Update exactly as AcademicSetupView sends it")
    @WithMockUser(username = "candidate.student@rknec.edu", roles = {"STUDENT"})
    void testStudentProfileUpdate_JsonRequestAsFrontend() throws Exception {
        Long studentId = demoStudent != null ? demoStudent.getId() : 1L;

        java.util.Map<String, Object> acadMap = new java.util.LinkedHashMap<>();
        acadMap.put("tenthPercentage", 85.0);
        acadMap.put("tenthCgpa", 8.5);
        acadMap.put("tenthBoard", "CBSE");
        acadMap.put("tenthYearOfPassing", 2020);
        acadMap.put("twelfthPercentage", 82.0);
        acadMap.put("twelfthCgpa", 8.2);
        acadMap.put("twelfthBoard", "State Board");
        acadMap.put("twelfthStream", "Science (PCM)");
        acadMap.put("twelfthYearOfPassing", 2022);
        acadMap.put("branch", "Computer Science & Engineering");
        acadMap.put("admissionYear", 2022);
        acadMap.put("expectedGraduationYear", 2026);
        acadMap.put("semesterSgpas", List.of(
                Map.of("semesterNumber", 1, "sgpa", 8.5, "isFinalized", true),
                Map.of("semesterNumber", 2, "sgpa", 8.8, "isFinalized", true)
        ));

        Map<String, Object> updatePayload = Map.of(
                "collegeRollNumber", "2022CS099",
                "phoneNumber", "9876543210",
                "placementStatus", "NOT_PLACED",
                "linkedinUrl", "https://linkedin.com/in/demo-student",
                "githubUrl", "https://github.com/demo-student",
                "academicProfile", acadMap
        );

        mockMvc.perform(put("/students/" + studentId + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId", equalTo(studentId.intValue())))
                .andExpect(jsonPath("$.collegeRollNumber", equalTo("2022CS099")))
                .andExpect(jsonPath("$.academicProfile.branch", equalTo("Computer Science & Engineering")));
    }

    @Test
    @DisplayName("Frontend Flow: Drive Application exactly as api.applyToDrive sends it")
    @WithMockUser(username = "candidate.student@rknec.edu", roles = {"STUDENT"})
    void testDriveApply_RequestAsFrontend() throws Exception {
        Long studentId = demoStudent != null ? demoStudent.getId() : 1L;

        mockMvc.perform(post("/drives/" + testDrive.getId() + "/apply")
                        .param("studentId", studentId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driveId", equalTo(testDrive.getId().intValue())))
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    @DisplayName("Frontend Flow: Application Status Override exactly as api.overrideApplicationStatus sends it")
    @WithMockUser(username = "admin@rknec.edu", roles = {"COORDINATOR"})
    void testOverrideApplicationStatus_JsonRequestAsFrontend() throws Exception {
        Long studentId = demoStudent != null ? demoStudent.getId() : 1L;

        Application app = applicationRepository.save(Application.builder()
                .drive(testDrive)
                .student(demoStudent)
                .status(ApplicationStatus.PENDING)
                .build());

        Map<String, String> overridePayload = Map.of(
                "status", "OVERRIDDEN_INCLUDED",
                "overrideReason", "Special recommendation by Placement Committee"
        );

        mockMvc.perform(post("/applications/" + app.getId() + "/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overridePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("OVERRIDDEN_INCLUDED")))
                .andExpect(jsonPath("$.overrideReason", equalTo("Special recommendation by Placement Committee")));
    }
}
