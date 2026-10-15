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
}
