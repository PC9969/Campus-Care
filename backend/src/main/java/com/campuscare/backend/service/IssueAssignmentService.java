package com.campuscare.backend.service;

import com.campuscare.backend.dto.IssueAssignmentResponse;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.exception.ResourceNotFoundException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.IssueAssignmentRepository;
import com.campuscare.backend.repository.IssueRepository;
import com.campuscare.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class IssueAssignmentService {

    private final IssueAssignmentRepository issueAssignmentRepository;
    private final IssueRepository issueRepository;
    private final ProfileRepository profileRepository;

    public IssueAssignmentService(
            IssueAssignmentRepository issueAssignmentRepository,
            IssueRepository issueRepository,
            ProfileRepository profileRepository
    ) {
        this.issueAssignmentRepository = issueAssignmentRepository;
        this.issueRepository = issueRepository;
        this.profileRepository = profileRepository;
    }

    public IssueAssignmentResponse assignStaff(Long issueId, UUID staffId, UUID assignedBy) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + issueId));

        Profile staffProfile = profileRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with ID: " + staffId));

        if (staffProfile.getRole() != UserRole.MAINTENANCE) {
            throw new BadRequestException("Selected user is not a Maintenance Staff member");
        }

        // Close/complete any previous active assignment for this issue
        Optional<IssueAssignment> existingActive = issueAssignmentRepository.findTopByIssueIdOrderByAssignedAtDesc(issueId);
        if (existingActive.isPresent() && existingActive.get().getCompletedAt() == null) {
            IssueAssignment prev = existingActive.get();
            if (prev.getStaffId().equals(staffId)) {
                // Already assigned to this technician
                return mapToResponse(prev, staffProfile.getName());
            }
            prev.setCompletedAt(OffsetDateTime.now());
            issueAssignmentRepository.save(prev);
        }

        IssueAssignment assignment = new IssueAssignment();
        assignment.setIssue(issue);
        assignment.setStaffId(staffId);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(OffsetDateTime.now());

        IssueAssignment saved = issueAssignmentRepository.save(assignment);

        // If the issue was OPEN, transitioning to IN_PROGRESS when assigned is a standard practical workflow
        if (issue.getStatus() == IssueStatus.OPEN) {
            issue.setStatus(IssueStatus.IN_PROGRESS);
            issue.setUpdatedAt(OffsetDateTime.now());
            issueRepository.save(issue);
        }

        return mapToResponse(saved, staffProfile.getName());
    }

    public void completeAssignment(Long issueId) {
        Optional<IssueAssignment> activeOpt = issueAssignmentRepository.findTopByIssueIdOrderByAssignedAtDesc(issueId);
        if (activeOpt.isPresent()) {
            IssueAssignment assignment = activeOpt.get();
            if (assignment.getCompletedAt() == null) {
                assignment.setCompletedAt(OffsetDateTime.now());
                issueAssignmentRepository.save(assignment);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<IssueAssignmentResponse> getAssignmentsByIssue(Long issueId, UUID currentUserId, UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.MAINTENANCE) {
            throw new UnauthorizedException("Students and professors cannot access assignment information");
        }

        if (role == UserRole.MAINTENANCE) {
            boolean isAssigned = issueAssignmentRepository.findByStaffId(currentUserId).stream()
                    .anyMatch(a -> a.getIssue().getId().equals(issueId));
            if (!isAssigned) {
                throw new UnauthorizedException("Maintenance staff can only view assignments for issues assigned to them");
            }
        }

        return getAssignmentsByIssue(issueId);
    }

    @Transactional(readOnly = true)
    public List<IssueAssignmentResponse> getAssignmentsByIssue(Long issueId) {
        return issueAssignmentRepository.findByIssueId(issueId).stream()
                .map(a -> {
                    String staffName = profileRepository.findById(a.getStaffId())
                            .map(Profile::getName)
                            .orElse("Technician");
                    return mapToResponse(a, staffName);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IssueAssignmentResponse> getAssignmentsByStaff(UUID staffId, UUID currentUserId, UserRole role) {
        if (role != UserRole.ADMIN && role != UserRole.MAINTENANCE) {
            throw new UnauthorizedException("Students and professors cannot access assignment information");
        }

        if (role == UserRole.MAINTENANCE && !currentUserId.equals(staffId)) {
            throw new UnauthorizedException("Maintenance staff can only view their own assignments");
        }

        return getAssignmentsByStaff(staffId);
    }

    @Transactional(readOnly = true)
    public List<IssueAssignmentResponse> getAssignmentsByStaff(UUID staffId) {
        String staffName = profileRepository.findById(staffId)
                .map(Profile::getName)
                .orElse("Technician");

        return issueAssignmentRepository.findByStaffId(staffId).stream()
                .map(a -> mapToResponse(a, staffName))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<IssueAssignment> getActiveAssignment(Long issueId) {
        return issueAssignmentRepository.findTopByIssueIdOrderByAssignedAtDesc(issueId)
                .filter(a -> a.getCompletedAt() == null);
    }

    private IssueAssignmentResponse mapToResponse(IssueAssignment a, String staffName) {
        IssueAssignmentResponse resp = new IssueAssignmentResponse();
        resp.setId(a.getId());
        resp.setIssueId(a.getIssue().getId());
        resp.setIssueTitle(a.getIssue().getTitle());
        resp.setStaffId(a.getStaffId());
        resp.setStaffName(staffName);
        resp.setAssignedBy(a.getAssignedBy());
        if (a.getAssignedBy() != null) {
            resp.setAssignedByName(profileRepository.findById(a.getAssignedBy())
                    .map(Profile::getName)
                    .orElse("Administrator"));
        }
        resp.setAssignedAt(a.getAssignedAt());
        resp.setCompletedAt(a.getCompletedAt());
        return resp;
    }
}
