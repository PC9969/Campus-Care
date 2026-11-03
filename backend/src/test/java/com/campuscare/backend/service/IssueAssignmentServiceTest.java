package com.campuscare.backend.service;

import com.campuscare.backend.dto.IssueAssignmentResponse;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.IssueAssignmentRepository;
import com.campuscare.backend.repository.IssueRepository;
import com.campuscare.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueAssignmentServiceTest {

    @Mock
    private IssueAssignmentRepository issueAssignmentRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProfileRepository profileRepository;

    private IssueAssignmentService issueAssignmentService;

    private UUID adminId;
    private UUID staffId;
    private UUID otherStaffId;
    private UUID studentId;
    private Issue issue;

    @BeforeEach
    void setUp() {
        issueAssignmentService = new IssueAssignmentService(
                issueAssignmentRepository,
                issueRepository,
                profileRepository
        );

        adminId = UUID.randomUUID();
        staffId = UUID.randomUUID();
        otherStaffId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        issue = new Issue();
        issue.setId(101L);
        issue.setTitle("Broken Elevator");
    }

    @Test
    void testStudentCannotAccessIssueAssignments() {
        assertThrows(UnauthorizedException.class, () ->
                issueAssignmentService.getAssignmentsByIssue(101L, studentId, UserRole.STUDENT)
        );
        verifyNoInteractions(issueAssignmentRepository);
    }

    @Test
    void testMaintenanceStaffCannotAccessUnassignedIssueAssignments() {
        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(Collections.emptyList());

        assertThrows(UnauthorizedException.class, () ->
                issueAssignmentService.getAssignmentsByIssue(101L, staffId, UserRole.MAINTENANCE)
        );
    }

    @Test
    void testMaintenanceStaffCanAccessAssignedIssueAssignments() {
        IssueAssignment assignment = new IssueAssignment();
        assignment.setId(1L);
        assignment.setIssue(issue);
        assignment.setStaffId(staffId);

        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(List.of(assignment));
        when(issueAssignmentRepository.findByIssueId(101L)).thenReturn(List.of(assignment));
        when(profileRepository.findById(staffId)).thenReturn(Optional.empty());

        List<IssueAssignmentResponse> resp = issueAssignmentService.getAssignmentsByIssue(101L, staffId, UserRole.MAINTENANCE);
        assertEquals(1, resp.size());
        assertEquals(101L, resp.get(0).getIssueId());
    }

    @Test
    void testStudentCannotAccessStaffAssignments() {
        assertThrows(UnauthorizedException.class, () ->
                issueAssignmentService.getAssignmentsByStaff(staffId, studentId, UserRole.STUDENT)
        );
        verifyNoInteractions(issueAssignmentRepository);
    }

    @Test
    void testMaintenanceStaffCannotAccessOtherStaffAssignments() {
        assertThrows(UnauthorizedException.class, () ->
                issueAssignmentService.getAssignmentsByStaff(otherStaffId, staffId, UserRole.MAINTENANCE)
        );
        verifyNoInteractions(issueAssignmentRepository);
    }

    @Test
    void testMaintenanceStaffCanAccessOwnStaffAssignments() {
        IssueAssignment assignment = new IssueAssignment();
        assignment.setId(2L);
        assignment.setIssue(issue);
        assignment.setStaffId(staffId);

        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(List.of(assignment));

        List<IssueAssignmentResponse> resp = issueAssignmentService.getAssignmentsByStaff(staffId, staffId, UserRole.MAINTENANCE);
        assertEquals(1, resp.size());
    }

    @Test
    void testAdminCanAccessAnyStaffAssignments() {
        IssueAssignment assignment = new IssueAssignment();
        assignment.setId(3L);
        assignment.setIssue(issue);
        assignment.setStaffId(staffId);

        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(List.of(assignment));

        List<IssueAssignmentResponse> resp = issueAssignmentService.getAssignmentsByStaff(staffId, adminId, UserRole.ADMIN);
        assertEquals(1, resp.size());
    }
}
