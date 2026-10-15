package com.placementos.tpms.controller;

import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.repository.ResumeVersionRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
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
    private ResumeVersionRepository resumeVersionRepository;

    private User ownerUser;
    private User strangerUser;
    private User coordUser;
    private ResumeVersion testResume;

    @BeforeEach
    void setUp() throws Exception {
        ownerUser = userRepository.save(User.builder()
                .name("Alice Owner")
                .email("alice.owner@rknec.edu")
                .password("encoded_pass")
                .role("STUDENT")
                .build());

        Student ownerStudent = studentRepository.save(Student.builder()
                .user(ownerUser)
                .collegeId("COLL_ALICE")
                .collegeRollNumber("ROLL_ALICE")
                .build());

        strangerUser = userRepository.save(User.builder()
                .name("Bob Stranger")
                .email("bob.stranger@rknec.edu")
                .password("encoded_pass")
                .role("STUDENT")
                .build());

        coordUser = userRepository.save(User.builder()
                .name("Charlie Coord")
                .email("charlie.coord@rknec.edu")
                .password("encoded_pass")
                .role("COORDINATOR")
                .build());

        userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@rknec.edu")
                .password("encoded_pass")
                .role("ADMIN")
                .build());

        // Create temporary PDF file for download testing
        File tempDir = new File("uploads/resumes/");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File tempPdf = new File(tempDir, "test_alice_resume.pdf");
        try (FileOutputStream fos = new FileOutputStream(tempPdf)) {
            fos.write("%PDF-1.4 sample content".getBytes());
        }

        testResume = resumeVersionRepository.save(ResumeVersion.builder()
                .student(ownerStudent)
                .filePath(tempPdf.getPath())
                .extractionStatus(ExtractionStatus.PROCESSED)
                .extractionSource("LLM")
                .build());
    }

    @Test
    void testResumeDownload_LoggedOut_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/resumes/" + testResume.getId() + "/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "bob.stranger@rknec.edu", roles = {"STUDENT"})
    void testResumeDownload_StrangerStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/resumes/" + testResume.getId() + "/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice.owner@rknec.edu", roles = {"STUDENT"})
    void testResumeDownload_OwnerStudent_ReturnsOk() throws Exception {
        mockMvc.perform(get("/resumes/" + testResume.getId() + "/download"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "charlie.coord@rknec.edu", roles = {"COORDINATOR"})
    void testResumeDownload_Coordinator_ReturnsOk() throws Exception {
        mockMvc.perform(get("/resumes/" + testResume.getId() + "/download"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@rknec.edu", roles = {"ADMIN"})
    void testResumeDownload_Admin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/resumes/" + testResume.getId() + "/download"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginValidation_EmptyFields_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterValidation_InvalidEmail_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"not-an-email\",\"password\":\"short\",\"role\":\"STUDENT\",\"collegeId\":\"C1\"}"))
                .andExpect(status().isBadRequest());
    }
}
