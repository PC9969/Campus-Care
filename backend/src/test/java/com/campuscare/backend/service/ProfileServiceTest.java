package com.campuscare.backend.service;

import com.campuscare.backend.dto.StaffResponse;
import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ProfessorProfileRepository professorProfileRepository;
    @Mock
    private MaintenanceProfileRepository maintenanceProfileRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private IssueAssignmentRepository issueAssignmentRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                profileRepository,
                studentProfileRepository,
                professorProfileRepository,
                maintenanceProfileRepository,
                issueRepository,
                issueAssignmentRepository
        );
    }

    @Test
    void testStudentProfileDynamicMetrics() {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(userId);
        profile.setName("Rahul Sharma");
        profile.setEmail("rahul.s@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        StudentProfile sp = new StudentProfile();
        sp.setUserId(userId);
        sp.setBranch("Computer Science & Engineering");

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(studentProfileRepository.findById(userId)).thenReturn(Optional.of(sp));
        when(issueRepository.countByReportedBy(userId)).thenReturn(5L);
        when(issueRepository.countByReportedByAndStatus(userId, IssueStatus.RESOLVED)).thenReturn(2L);
        when(issueRepository.countByReportedByAndStatus(userId, IssueStatus.CLOSED)).thenReturn(1L);

        UserProfileResponse resp = profileService.getCurrentUserProfile(userId);

        assertNotNull(resp);
        assertEquals("Rahul Sharma", resp.getName());
        assertEquals("Student", resp.getRole());
        assertEquals("Computer Science & Engineering", resp.getBranch());
        assertEquals(5L, resp.getIssuesReported());
        assertEquals(3L, resp.getIssuesResolved());
    }

    @Test
    void testMaintenanceStaffWorkloadCalculation() {
        UUID staffId = UUID.randomUUID();
        Profile staff = new Profile();
        staff.setId(staffId);
        staff.setName("Amit Kumar");
        staff.setEmail("amit.k@srmist.edu.in");
        staff.setRole(UserRole.MAINTENANCE);

        MaintenanceProfile mp = new MaintenanceProfile();
        mp.setUserId(staffId);
        mp.setStaffId("STF-SRM-019");
        mp.setField("Electrical");

        when(profileRepository.findByRole(UserRole.MAINTENANCE)).thenReturn(List.of(staff));
        when(maintenanceProfileRepository.findById(staffId)).thenReturn(Optional.of(mp));
        when(issueAssignmentRepository.countByStaffId(staffId)).thenReturn(10L);
        when(issueAssignmentRepository.countByStaffIdAndCompletedAtIsNotNull(staffId)).thenReturn(8L);
        when(issueAssignmentRepository.countByStaffIdAndCompletedAtIsNull(staffId)).thenReturn(2L);

        List<StaffResponse> staffList = profileService.getAllStaff();

        assertEquals(1, staffList.size());
        StaffResponse s = staffList.get(0);
        assertEquals("Amit Kumar", s.getName());
        assertEquals("Electrical", s.getField());
        assertEquals("STF-SRM-019", s.getStaffId());
        assertEquals(10L, s.getIssuesAssigned());
        assertEquals(8L, s.getIssuesSolved());
        assertEquals(2L, s.getActiveIssues());
        assertEquals("Moderate", s.getWorkload());
        assertEquals("On Task", s.getStatus());
    }
}
