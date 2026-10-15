package com.placementos.internship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.internship.config.DemoAccountProperties;
import com.placementos.internship.dto.InternshipSubmissionDTO;
import com.placementos.internship.dto.PolicyRuleDTO;
import com.placementos.internship.dto.RejectionRequestDTO;
import com.placementos.internship.entity.*;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.StudentRepository;
import com.placementos.internship.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private InternshipRequestRepository requestRepository;

    private User demoStudentUser;
    private Student demoStudent;
    private InternshipRequest testRequest;

    @BeforeEach
    void setUp() {
        DemoAccountProperties.DemoAccountConfig studentConfig = demoProperties.getAccounts().stream()
                .filter(a -> "STUDENT".equalsIgnoreCase(a.getRole()))
                .findFirst()
                .orElse(null);

        String email = studentConfig != null ? studentConfig.getEmail() : "student1@rknec.edu";
        demoStudentUser = userRepository.findByEmail(email).orElse(null);
        if (demoStudentUser == null) {
            demoStudentUser = userRepository.save(User.builder()
                    .name("Rahul Sharma")
                    .email(email)
                    .password("$2a$10$w0...fakeEncodedPassword")
                    .role(Role.STUDENT)
                    .build());
        }

        demoStudent = studentRepository.findByUserId(demoStudentUser.getId()).orElse(null);
        if (demoStudent == null) {
            demoStudent = studentRepository.save(Student.builder()
                    .user(demoStudentUser)
                    .department("Computer Science")
                    .gpa(8.5)
                    .build());
        }

        testRequest = requestRepository.save(InternshipRequest.builder()
                .student(demoStudent)
                .companyName("Acme Corp")
                .roleTitle("Software Engineer Intern")
                .durationWeeks(12)
                .stipend(new BigDecimal("30000.00"))
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(94))
                .mode(InternshipMode.HYBRID)
                .hrContactEmail("hr@acme.com")
                .status(InternshipRequestStatus.SUBMITTED)
                .build());
    }

    @Test
    @DisplayName("Frontend Auth Flow: Login using demo student credentials")
    void testDemoStudentLoginFlow() throws Exception {
        DemoAccountProperties.DemoAccountConfig studentConfig = demoProperties.getAccounts().stream()
                .filter(a -> "STUDENT".equalsIgnoreCase(a.getRole()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No demo student in properties"));

        Map<String, String> loginRequest = Map.of(
                "email", studentConfig.getEmail(),
                "password", studentConfig.getPassword()
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email", is(studentConfig.getEmail())))
                .andExpect(jsonPath("$.role", is("STUDENT")));
    }

    @Test
    @DisplayName("Frontend Internship Submission Flow: Multipart offer submission using part name 'dto'")
    @WithMockUser(username = "student1@rknec.edu", roles = {"STUDENT"})
    void testOfferSubmissionMultipartFlow_WithDtoPart() throws Exception {
        InternshipSubmissionDTO submissionDto = InternshipSubmissionDTO.builder()
                .studentId(demoStudent.getId())
                .companyName("Tech Innovators Inc")
                .roleTitle("Backend Intern")
                .durationWeeks(16)
                .stipend(new BigDecimal("25000.00"))
                .startDate(LocalDate.now().plusDays(15))
                .endDate(LocalDate.now().plusDays(127))
                .mode(InternshipMode.REMOTE)
                .hrContactEmail("careers@techinnovators.com")
                .build();

        MockMultipartFile dtoPart = new MockMultipartFile(
                "dto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(submissionDto)
        );

        byte[] validPdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes();
        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "offer_letter.pdf",
                "application/pdf",
                validPdfBytes
        );

        mockMvc.perform(multipart("/internships/submit")
                        .file(dtoPart)
                        .file(filePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName", is("Tech Innovators Inc")))
                .andExpect(jsonPath("$.roleTitle", is("Backend Intern")))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("Frontend Review Queue Flow: Fetch pending internship submissions")
    @WithMockUser(username = "admin@rknec.edu", roles = {"CDPC_ADMIN"})
    void testReviewQueueListingFlow() throws Exception {
        mockMvc.perform(get("/internships/review-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[?(@.companyName == 'Acme Corp')]").exists());
    }

    @Test
    @DisplayName("Frontend Review Flow: Approve an internship submission")
    @WithMockUser(username = "admin@rknec.edu", roles = {"CDPC_ADMIN"})
    void testApproveRequestFlow() throws Exception {
        mockMvc.perform(post("/internships/" + testRequest.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testRequest.getId().intValue())))
                .andExpect(jsonPath("$.status", is("MANUALLY_APPROVED")));
    }

    @Test
    @DisplayName("Frontend Review Flow: Reject an internship submission with JSON reason")
    @WithMockUser(username = "admin@rknec.edu", roles = {"CDPC_ADMIN"})
    void testRejectRequestFlow() throws Exception {
        RejectionRequestDTO rejectionDto = new RejectionRequestDTO("Stipend below department threshold criteria.");

        mockMvc.perform(post("/internships/" + testRequest.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testRequest.getId().intValue())))
                .andExpect(jsonPath("$.status", is("MANUALLY_REJECTED")));
    }

    @Test
    @DisplayName("Frontend Policy Rules Flow: Fetch and create policy rules")
    @WithMockUser(username = "admin@rknec.edu", roles = {"CDPC_ADMIN"})
    void testPolicyRulesGetAndPostFlow() throws Exception {
        mockMvc.perform(get("/policy-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));

        PolicyRuleDTO newRule = PolicyRuleDTO.builder()
                .ruleName("RULE_MIN_WEEKS_AUTO")
                .label("Minimum Internship Duration")
                .ruleType("INTEGER")
                .ruleValue("8")
                .isActive(true)
                .description("Requires minimum 8 weeks duration")
                .build();

        mockMvc.perform(post("/policy-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRule)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleName", is("RULE_MIN_WEEKS_AUTO")))
                .andExpect(jsonPath("$.ruleValue", is("8")));
    }
}
