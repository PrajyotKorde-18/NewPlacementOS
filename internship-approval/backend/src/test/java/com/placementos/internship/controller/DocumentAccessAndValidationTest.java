package com.placementos.internship.controller;

import com.placementos.internship.entity.InternshipMode;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.InternshipRequestStatus;
import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.Student;
import com.placementos.internship.entity.User;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.StudentRepository;
import com.placementos.internship.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DocumentAccessAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InternshipRequestRepository requestRepository;

    private User ownerUser;
    private User strangerUser;
    private User staffUser;
    private InternshipRequest testRequest;

    @BeforeEach
    void setUp() throws Exception {
        ownerUser = userRepository.save(User.builder()
                .name("Student Alice")
                .email("alice@rknec.edu")
                .password("encoded_pass")
                .role(Role.STUDENT)
                .collegeId("COLL_A")
                .build());

        Student student = studentRepository.save(Student.builder()
                .user(ownerUser)
                .department("CSE")
                .gpa(8.5)
                .build());

        strangerUser = userRepository.save(User.builder()
                .name("Student Bob")
                .email("bob@rknec.edu")
                .password("encoded_pass")
                .role(Role.STUDENT)
                .collegeId("COLL_B")
                .build());

        staffUser = userRepository.save(User.builder()
                .name("Staff Member")
                .email("staff@rknec.edu")
                .password("encoded_pass")
                .role(Role.CDPC_STAFF)
                .collegeId("STAFF_1")
                .build());

        File dir = new File("uploads/offers/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File offerPdf = new File(dir, "offer_alice_test.pdf");
        try (FileOutputStream fos = new FileOutputStream(offerPdf)) {
            fos.write("%PDF-1.4 test offer letter content".getBytes());
        }

        testRequest = requestRepository.save(InternshipRequest.builder()
                .student(student)
                .companyName("Acme Corp")
                .roleTitle("Software Intern")
                .stipend(java.math.BigDecimal.valueOf(15000))
                .durationWeeks(8)
                .mode(InternshipMode.HYBRID)
                .hrContactEmail("hr@acmepune.com")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(60))
                .offerLetterUrl(offerPdf.getPath())
                .status(InternshipRequestStatus.RULE_APPROVED)
                .build());

        File nocDir = new File("uploads/noc/");
        if (!nocDir.exists()) {
            nocDir.mkdirs();
        }
        File nocPdf = new File(nocDir, "NOC_Request_" + testRequest.getId() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(nocPdf)) {
            fos.write("%PDF-1.4 test noc content".getBytes());
        }
    }

    @Test
    void testOfferLetterDownload_LoggedOut_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/offer-letter"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "bob@rknec.edu", roles = {"STUDENT"})
    void testOfferLetterDownload_StrangerStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/offer-letter"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@rknec.edu", roles = {"STUDENT"})
    void testOfferLetterDownload_OwnerStudent_ReturnsOk() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/offer-letter"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@rknec.edu", roles = {"CDPC_STAFF"})
    void testOfferLetterDownload_CdpcStaff_ReturnsOk() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/offer-letter"))
                .andExpect(status().isOk());
    }

    @Test
    void testNocFileDownload_LoggedOut_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/noc-file"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "bob@rknec.edu", roles = {"STUDENT"})
    void testNocFileDownload_StrangerStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/noc-file"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@rknec.edu", roles = {"STUDENT"})
    void testNocFileDownload_OwnerStudent_ReturnsOk() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/noc-file"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@rknec.edu", roles = {"CDPC_STAFF"})
    void testNocFileDownload_CdpcStaff_ReturnsOk() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/noc-file"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@rknec.edu", roles = {"CDPC_ADMIN"})
    void testNocFileDownload_CdpcAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/internships/" + testRequest.getId() + "/noc-file"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginValidation_EmptyCredentials_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterValidation_InvalidFields_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-valid\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice@rknec.edu", roles = {"STUDENT"})
    void testSubmitOffer_FrontendMultipartFormat_Accepted() throws Exception {
        String jsonPayload = """
                {
                    "studentId": %d,
                    "companyName": "Tech Innovators Inc",
                    "roleTitle": "Software Intern",
                    "durationWeeks": 12,
                    "stipend": 25000,
                    "startDate": "2026-06-01",
                    "endDate": "2026-08-31",
                    "mode": "ON_SITE",
                    "hrContactEmail": "hr@techinnovators.com"
                }
                """.formatted(ownerUser.getId());

        org.springframework.mock.web.MockMultipartFile dtoPart = new org.springframework.mock.web.MockMultipartFile(
                "dto",
                "",
                "application/json",
                jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        org.springframework.mock.web.MockMultipartFile filePart = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "sample_offer.pdf",
                "application/pdf",
                "%PDF-1.4 sample genuine pdf content".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/internships/submit")
                        .file(dtoPart)
                        .file(filePart))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "alice@rknec.edu", roles = {"STUDENT"})
    void testSubmitOffer_ValidPdfGeneratedByPdfBox_Accepted() throws Exception {
        byte[] pdfBytes;
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            document.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Official Internship Offer Letter");
                contentStream.endText();
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            pdfBytes = baos.toByteArray();
        }

        String jsonPayload = """
                {
                    "studentId": %d,
                    "companyName": "Enterprise Solutions Corp",
                    "roleTitle": "Backend Engineer Intern",
                    "durationWeeks": 10,
                    "stipend": 30000,
                    "startDate": "2026-07-01",
                    "endDate": "2026-09-15",
                    "mode": "HYBRID",
                    "hrContactEmail": "hr@enterprisesolutions.com"
                }
                """.formatted(ownerUser.getId());

        org.springframework.mock.web.MockMultipartFile dtoPart = new org.springframework.mock.web.MockMultipartFile(
                "dto",
                "",
                "application/json",
                jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        org.springframework.mock.web.MockMultipartFile filePart = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "enterprise_offer.pdf",
                "application/pdf",
                pdfBytes
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/internships/submit")
                        .file(dtoPart)
                        .file(filePart))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "alice@rknec.edu", roles = {"STUDENT"})
    void testSubmitOffer_CorruptOrInvalidPdf_ReturnsBadRequest() throws Exception {
        String jsonPayload = """
                {
                    "studentId": %d,
                    "companyName": "Fake Corp",
                    "roleTitle": "Intern",
                    "durationWeeks": 8,
                    "stipend": 20000,
                    "startDate": "2026-06-01",
                    "endDate": "2026-08-01",
                    "mode": "ON_SITE",
                    "hrContactEmail": "hr@fakecorp.com"
                }
                """.formatted(ownerUser.getId());

        org.springframework.mock.web.MockMultipartFile dtoPart = new org.springframework.mock.web.MockMultipartFile(
                "dto",
                "",
                "application/json",
                jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        org.springframework.mock.web.MockMultipartFile fakeFile = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "NOT_A_REAL_PDF_HEADER_CONTENT".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/internships/submit")
                        .file(dtoPart)
                        .file(fakeFile))
                .andExpect(status().isBadRequest());
    }
}
