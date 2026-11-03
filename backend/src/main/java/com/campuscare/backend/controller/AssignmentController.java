package com.campuscare.backend.controller;

import com.campuscare.backend.dto.IssueAssignmentRequest;
import com.campuscare.backend.dto.IssueAssignmentResponse;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.IssueAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final IssueAssignmentService issueAssignmentService;

    public AssignmentController(IssueAssignmentService issueAssignmentService) {
        this.issueAssignmentService = issueAssignmentService;
    }

    @PostMapping
    public ResponseEntity<IssueAssignmentResponse> assignStaff(
            @Valid @RequestBody IssueAssignmentRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only administrators can assign issues");
        }

        IssueAssignmentResponse response = issueAssignmentService.assignStaff(
                request.getIssueId(),
                request.getStaffId(),
                principal.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<IssueAssignmentResponse>> getAssignmentsByIssue(
            @PathVariable Long issueId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal.getRole() != UserRole.ADMIN && principal.getRole() != UserRole.MAINTENANCE) {
            throw new UnauthorizedException("Students and professors cannot access assignment information");
        }

        List<IssueAssignmentResponse> assignments = issueAssignmentService.getAssignmentsByIssue(
                issueId,
                principal.getId(),
                principal.getRole()
        );
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<IssueAssignmentResponse>> getAssignmentsByStaff(
            @PathVariable UUID staffId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal.getRole() != UserRole.ADMIN && principal.getRole() != UserRole.MAINTENANCE) {
            throw new UnauthorizedException("Students and professors cannot access assignment information");
        }

        if (principal.getRole() == UserRole.MAINTENANCE && !principal.getId().equals(staffId)) {
            throw new UnauthorizedException("Maintenance staff can only view their own assignments");
        }

        List<IssueAssignmentResponse> assignments = issueAssignmentService.getAssignmentsByStaff(
                staffId,
                principal.getId(),
                principal.getRole()
        );
        return ResponseEntity.ok(assignments);
    }
}
