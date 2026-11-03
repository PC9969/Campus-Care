package com.campuscare.backend.service;

import com.campuscare.backend.dto.CreateIssueRequest;
import com.campuscare.backend.dto.IssueResponse;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.exception.InvalidWorkflowTransitionException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private IssueCategoryRepository issueCategoryRepository;
    @Mock
    private IssueAssignmentRepository issueAssignmentRepository;
    @Mock
    private IssueEvidenceRepository issueEvidenceRepository;
    @Mock
    private IssueStatusHistoryRepository issueStatusHistoryRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private IssueCategoryService issueCategoryService;
    @Mock
    private IssueAssignmentService issueAssignmentService;
    @Mock
    private IssueEvidenceService issueEvidenceService;

    private IssueService issueService;

    private UUID studentId;
    private UUID staffId;
    private Profile studentProfile;
    private IssueCategory category;

    @BeforeEach
    void setUp() {
        issueService = new IssueService(
                issueRepository,
                issueCategoryRepository,
                issueAssignmentRepository,
                issueEvidenceRepository,
                issueStatusHistoryRepository,
                profileRepository,
                issueCategoryService,
                issueAssignmentService,
                issueEvidenceService
        );

        studentId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        studentProfile = new Profile();
        studentProfile.setId(studentId);
        studentProfile.setName("Rahul Sharma");
        studentProfile.setEmail("rahul.s@srmist.edu.in");
        studentProfile.setRole(UserRole.STUDENT);

        category = new IssueCategory();
        category.setId(1L);
        category.setName("Plumbing");
    }

    @Test
    void testCreateIssueSuccessPreservesInitialHistory() {
        CreateIssueRequest req = new CreateIssueRequest();
        req.setTitle("Water leakage in lab");
        req.setCategory("Plumbing");
        req.setLocation("Ground Floor Fluids Lab");
        req.setPriority("High");
        req.setDescription("Water dripping near door");

        when(profileRepository.findById(studentId)).thenReturn(Optional.of(studentProfile));
        when(issueCategoryService.findOrCreateCategory("Plumbing")).thenReturn(category);

        Issue savedIssue = new Issue();
        savedIssue.setId(101L);
        savedIssue.setTitle(req.getTitle());
        savedIssue.setReportedBy(studentId);
        savedIssue.setCategory(category);
        savedIssue.setLocation(req.getLocation());
        savedIssue.setPriority(IssuePriority.HIGH);
        savedIssue.setStatus(IssueStatus.OPEN);
        savedIssue.setCreatedAt(OffsetDateTime.now());
        savedIssue.setUpdatedAt(OffsetDateTime.now());

        when(issueRepository.save(any(Issue.class))).thenReturn(savedIssue);

        IssueResponse resp = issueService.createIssue(req, studentId);

        assertNotNull(resp);
        assertEquals("ISS-101", resp.getId());
        assertEquals("Water leakage in lab", resp.getTitle());
        assertEquals("Open", resp.getStatus());
        assertEquals("High", resp.getPriority());

        // Initial status history MUST be preserved on creation
        verify(issueStatusHistoryRepository, times(1)).save(any(IssueStatusHistory.class));
        // Verify evidence metadata is not created without file upload
        verify(issueEvidenceService, never()).recordEvidenceMetadata(any(), any(), any(), any());
    }

    @Test
    void testDuplicateStatusHistoryPreventionOnStatusUpdate() {
        Issue issue = new Issue();
        issue.setId(201L);
        issue.setTitle("Broken light");
        issue.setStatus(IssueStatus.OPEN);
        issue.setReportedBy(studentId);
        issue.setCategory(category);
        issue.setPriority(IssuePriority.MEDIUM);
        issue.setLocation("Room 101");

        when(issueRepository.findById(201L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Perform status update to IN_PROGRESS
        IssueResponse inProgressResp = issueService.updateIssueStatus(201L, IssueStatus.IN_PROGRESS, "Started work", UUID.randomUUID(), UserRole.ADMIN);
        assertEquals("In Progress", inProgressResp.getStatus());

        // Assert that IssueService does NOT insert into issueStatusHistoryRepository on status update,
        // relying strictly on the existing Supabase database trigger to prevent duplicates.
        verifyNoInteractions(issueStatusHistoryRepository);
    }

    @Test
    void testValidStatusWorkflowTransitions() {
        Issue issue = new Issue();
        issue.setId(201L);
        issue.setTitle("Broken light");
        issue.setStatus(IssueStatus.OPEN);
        issue.setReportedBy(studentId);
        issue.setCategory(category);
        issue.setPriority(IssuePriority.MEDIUM);
        issue.setLocation("Room 101");

        when(issueRepository.findById(201L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // OPEN -> IN_PROGRESS succeeds for Admin
        IssueResponse inProgressResp = issueService.updateIssueStatus(201L, IssueStatus.IN_PROGRESS, "Started work", UUID.randomUUID(), UserRole.ADMIN);
        assertEquals("In Progress", inProgressResp.getStatus());

        // IN_PROGRESS -> RESOLVED succeeds and sets resolvedAt
        IssueResponse resolvedResp = issueService.updateIssueStatus(201L, IssueStatus.RESOLVED, "Repaired", UUID.randomUUID(), UserRole.ADMIN);
        assertEquals("Resolved", resolvedResp.getStatus());
        assertNotNull(resolvedResp.getResolvedAt());
        verify(issueAssignmentService).completeAssignment(201L);
    }

    @Test
    void testInvalidStatusWorkflowTransitionThrowsException() {
        Issue issue = new Issue();
        issue.setId(301L);
        issue.setStatus(IssueStatus.OPEN);
        issue.setReportedBy(studentId);

        when(issueRepository.findById(301L)).thenReturn(Optional.of(issue));

        // OPEN -> CLOSED directly is invalid for non-admin
        assertThrows(InvalidWorkflowTransitionException.class, () ->
                issueService.updateIssueStatus(301L, IssueStatus.CLOSED, "Direct close", studentId, UserRole.STUDENT)
        );
    }

    @Test
    void testStudentCannotViewOtherUsersIssues() {
        UUID otherUser = UUID.randomUUID();
        Issue issue = new Issue();
        issue.setId(401L);
        issue.setReportedBy(otherUser);

        when(issueRepository.findById(401L)).thenReturn(Optional.of(issue));

        assertThrows(UnauthorizedException.class, () ->
                issueService.getIssueById(401L, studentId, UserRole.STUDENT)
        );
    }

    @Test
    void testOnlyAdminCanAssignStaff() {
        Issue issue = new Issue();
        issue.setId(501L);
        issue.setTitle("AC repair");
        issue.setStatus(IssueStatus.OPEN);

        UUID adminId = UUID.randomUUID();

        // Non-admin roles must be rejected
        assertThrows(UnauthorizedException.class, () ->
                issueService.assignIssue(501L, staffId, studentId, UserRole.STUDENT)
        );
        assertThrows(UnauthorizedException.class, () ->
                issueService.assignIssue(501L, staffId, staffId, UserRole.MAINTENANCE)
        );
        assertThrows(UnauthorizedException.class, () ->
                issueService.assignIssue(501L, staffId, studentId, UserRole.PROFESSOR)
        );

        // Admin role succeeds
        when(issueRepository.findById(501L)).thenReturn(Optional.of(issue));
        IssueResponse resp = issueService.assignIssue(501L, staffId, adminId, UserRole.ADMIN);
        assertNotNull(resp);
        verify(issueAssignmentService).assignStaff(501L, staffId, adminId);
    }
}
