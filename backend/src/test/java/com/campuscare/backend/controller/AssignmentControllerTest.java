package com.campuscare.backend.controller;

import com.campuscare.backend.dto.IssueAssignmentRequest;
import com.campuscare.backend.dto.IssueAssignmentResponse;
import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.IssueAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    @Mock
    private IssueAssignmentService issueAssignmentService;

    private AssignmentController assignmentController;

    private UUID adminId;
    private UUID staffId;
    private UUID otherStaffId;
    private UUID studentId;
    private CustomUserPrincipal adminPrincipal;
    private CustomUserPrincipal staffPrincipal;
    private CustomUserPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        assignmentController = new AssignmentController(issueAssignmentService);

        adminId = UUID.randomUUID();
        staffId = UUID.randomUUID();
        otherStaffId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        Profile adminProfile = new Profile();
        adminProfile.setId(adminId);
        adminProfile.setRole(UserRole.ADMIN);
        adminPrincipal = new CustomUserPrincipal(adminProfile);

        Profile staffProfile = new Profile();
        staffProfile.setId(staffId);
        staffProfile.setRole(UserRole.MAINTENANCE);
        staffPrincipal = new CustomUserPrincipal(staffProfile);

        Profile studentProfile = new Profile();
        studentProfile.setId(studentId);
        studentProfile.setRole(UserRole.STUDENT);
        studentPrincipal = new CustomUserPrincipal(studentProfile);
    }

    @Test
    void testAdminCanAssignStaff() {
        IssueAssignmentRequest req = new IssueAssignmentRequest(10L, staffId);
        IssueAssignmentResponse resp = new IssueAssignmentResponse();
        resp.setIssueId(10L);
        resp.setStaffId(staffId);

        when(issueAssignmentService.assignStaff(10L, staffId, adminId)).thenReturn(resp);

        ResponseEntity<IssueAssignmentResponse> response = assignmentController.assignStaff(req, adminPrincipal);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getIssueId());
    }

    @Test
    void testMaintenanceStaffCannotAssignStaff() {
        IssueAssignmentRequest req = new IssueAssignmentRequest(10L, staffId);

        assertThrows(UnauthorizedException.class, () ->
                assignmentController.assignStaff(req, staffPrincipal)
        );
        verifyNoInteractions(issueAssignmentService);
    }

    @Test
    void testStudentCannotAssignStaff() {
        IssueAssignmentRequest req = new IssueAssignmentRequest(10L, staffId);

        assertThrows(UnauthorizedException.class, () ->
                assignmentController.assignStaff(req, studentPrincipal)
        );
        verifyNoInteractions(issueAssignmentService);
    }

    @Test
    void testStudentCannotAccessIssueAssignments() {
        assertThrows(UnauthorizedException.class, () ->
                assignmentController.getAssignmentsByIssue(10L, studentPrincipal)
        );
        verifyNoInteractions(issueAssignmentService);
    }

    @Test
    void testStudentCannotAccessStaffAssignments() {
        assertThrows(UnauthorizedException.class, () ->
                assignmentController.getAssignmentsByStaff(staffId, studentPrincipal)
        );
        verifyNoInteractions(issueAssignmentService);
    }

    @Test
    void testMaintenanceStaffCannotAccessOtherStaffAssignments() {
        assertThrows(UnauthorizedException.class, () ->
                assignmentController.getAssignmentsByStaff(otherStaffId, staffPrincipal)
        );
        verifyNoInteractions(issueAssignmentService);
    }

    @Test
    void testMaintenanceStaffCanAccessOwnStaffAssignments() {
        when(issueAssignmentService.getAssignmentsByStaff(staffId, staffId, UserRole.MAINTENANCE))
                .thenReturn(List.of(new IssueAssignmentResponse()));

        ResponseEntity<List<IssueAssignmentResponse>> resp = assignmentController.getAssignmentsByStaff(staffId, staffPrincipal);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void testAdminCanAccessAnyStaffAssignments() {
        when(issueAssignmentService.getAssignmentsByStaff(staffId, adminId, UserRole.ADMIN))
                .thenReturn(List.of(new IssueAssignmentResponse()));

        ResponseEntity<List<IssueAssignmentResponse>> resp = assignmentController.getAssignmentsByStaff(staffId, adminPrincipal);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().size());
    }
}
