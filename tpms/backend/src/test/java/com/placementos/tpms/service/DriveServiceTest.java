package com.placementos.tpms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.ApplicationResponseDTO;
import com.placementos.tpms.dto.ScoreBreakdownDTO;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.repository.*;
import com.placementos.tpms.scoring.ScoringEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveServiceTest {

    @Mock
    private DriveRepository driveRepository;
    @Mock
    private RoleProfileRepository roleProfileRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ScoringEngineService scoringEngineService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DriveService driveService;

    private Drive drive;
    private Student student1;
    private Student student2;
    private Application app1;
    private Application app2;

    @BeforeEach
    void setUp() {
        User creator = User.builder().id(1L).name("Admin").build();
        drive = Drive.builder().id(100L).collegeId("RKNECO1").companyName("Google").roleTitle("SWE").vacancies(1).createdBy(creator).build();

        User user1 = User.builder().id(10L).name("Alice").email("alice@rknec.edu").build();
        student1 = Student.builder().id(101L).user(user1).collegeId("C001").build();

        User user2 = User.builder().id(20L).name("Bob").email("bob@rknec.edu").build();
        student2 = Student.builder().id(102L).user(user2).collegeId("C002").build();

        app1 = Application.builder().id(1L).drive(drive).student(student1).status(ApplicationStatus.PENDING).build();
        app2 = Application.builder().id(2L).drive(drive).student(student2).status(ApplicationStatus.PENDING).build();
    }

    @Test
    @DisplayName("Auto-Shortlisting: Assigns SHORTLISTED to rank 1 and NOT_SHORTLISTED beyond vacancies")
    void testCalculateAndRankApplications_AutoShortlistsTopVacancy() {
        List<Application> apps = new ArrayList<>(List.of(app1, app2));
        when(driveRepository.findById(100L)).thenReturn(Optional.of(drive));
        when(applicationRepository.findByDriveIdOrderByScoreDesc(100L)).thenReturn(apps);
        when(roleProfileRepository.findByDriveId(100L)).thenReturn(Optional.empty());

        when(scoringEngineService.computeScoreBreakdown(any(), any()))
                .thenReturn(ScoreBreakdownDTO.builder().skillMatchScore(80.0).totalWeightedScore(90.0).build())
                .thenReturn(ScoreBreakdownDTO.builder().skillMatchScore(60.0).totalWeightedScore(70.0).build());

        when(applicationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ApplicationResponseDTO> ranked = driveService.calculateAndRankApplications(100L);

        assertNotNull(ranked);
        assertEquals(2, ranked.size());

        // Higher score app should be rank 1 and SHORTLISTED (vacancies = 1)
        assertEquals(1, ranked.get(0).getRankPosition());
        assertEquals(ApplicationStatus.SHORTLISTED, ranked.get(0).getStatus());

        // Lower score app should be rank 2 and NOT_SHORTLISTED
        assertEquals(2, ranked.get(1).getRankPosition());
        assertEquals(ApplicationStatus.NOT_SHORTLISTED, ranked.get(1).getStatus());
    }

    @Test
    @DisplayName("Override Preservation: Re-ranking preserves OVERRIDDEN_INCLUDED and OVERRIDDEN_EXCLUDED statuses")
    void testCalculateAndRankApplications_PreservesManualCoordinatorOverrides() {
        // App1 (rank 1, highest score) was manually OVERRIDDEN_EXCLUDED
        app1.setStatus(ApplicationStatus.OVERRIDDEN_EXCLUDED);
        app1.setOverrideReason("Disqualified due to policy violation");

        // App2 (rank 2, lower score) was manually OVERRIDDEN_INCLUDED by coordinator
        app2.setStatus(ApplicationStatus.OVERRIDDEN_INCLUDED);
        app2.setOverrideReason("Special candidate inclusion");

        List<Application> apps = new ArrayList<>(List.of(app1, app2));
        when(driveRepository.findById(100L)).thenReturn(Optional.of(drive));
        when(applicationRepository.findByDriveIdOrderByScoreDesc(100L)).thenReturn(apps);
        when(roleProfileRepository.findByDriveId(100L)).thenReturn(Optional.empty());

        when(scoringEngineService.computeScoreBreakdown(any(), any()))
                .thenReturn(ScoreBreakdownDTO.builder().skillMatchScore(80.0).totalWeightedScore(90.0).build())
                .thenReturn(ScoreBreakdownDTO.builder().skillMatchScore(60.0).totalWeightedScore(70.0).build());

        when(applicationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ApplicationResponseDTO> ranked = driveService.calculateAndRankApplications(100L);

        assertNotNull(ranked);
        assertEquals(2, ranked.size());

        // App1 at rank 1 MUST retain OVERRIDDEN_EXCLUDED status
        assertEquals(1, ranked.get(0).getRankPosition());
        assertEquals(ApplicationStatus.OVERRIDDEN_EXCLUDED, ranked.get(0).getStatus());
        assertEquals("Disqualified due to policy violation", ranked.get(0).getOverrideReason());

        // App2 at rank 2 MUST retain OVERRIDDEN_INCLUDED status even though vacancies = 1
        assertEquals(2, ranked.get(1).getRankPosition());
        assertEquals(ApplicationStatus.OVERRIDDEN_INCLUDED, ranked.get(1).getStatus());
        assertEquals("Special candidate inclusion", ranked.get(1).getOverrideReason());
    }

    @Test
    @DisplayName("Delete Drive: Deletes associated RoleProfile, Applications, and Drive entity from database")
    void testDeleteDrive_DeletesRelatedEntitiesAndDrive() {
        when(driveRepository.findById(100L)).thenReturn(Optional.of(drive));
        when(roleProfileRepository.findByDriveId(100L)).thenReturn(Optional.of(RoleProfile.builder().id(10L).drive(drive).build()));
        when(applicationRepository.findByDriveIdOrderByScoreDesc(100L)).thenReturn(List.of(app1, app2));

        assertDoesNotThrow(() -> driveService.deleteDrive(100L));

        org.mockito.Mockito.verify(roleProfileRepository).delete(any(RoleProfile.class));
        org.mockito.Mockito.verify(applicationRepository).deleteAll(any());
        org.mockito.Mockito.verify(driveRepository).delete(drive);
    }
}
