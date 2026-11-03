package com.campuscare.backend.controller;

import com.campuscare.backend.dto.*;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.IssueCategoryService;
import com.campuscare.backend.service.IssueEvidenceService;
import com.campuscare.backend.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;
    private final IssueCategoryService issueCategoryService;
    private final IssueEvidenceService issueEvidenceService;

    public IssueController(
            IssueService issueService,
            IssueCategoryService issueCategoryService,
            IssueEvidenceService issueEvidenceService
    ) {
        this.issueService = issueService;
        this.issueCategoryService = issueCategoryService;
        this.issueEvidenceService = issueEvidenceService;
    }

    @GetMapping
    public ResponseEntity<List<IssueResponse>> getIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<IssueResponse> issues = issueService.getAllIssues(
                principal.getId(),
                principal.getRole(),
                status,
                category,
                priority,
                search
        );
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/my")
    public ResponseEntity<List<IssueResponse>> getMyIssues(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<IssueResponse> issues = issueService.getMyIssues(principal.getId());
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/summary")
    public ResponseEntity<IssueSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        IssueSummaryResponse summary = issueService.getSummary(principal.getId(), principal.getRole());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = issueCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssueById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long numericId = parseId(id);
        IssueResponse issue = issueService.getIssueById(numericId, principal.getId(), principal.getRole());
        return ResponseEntity.ok(issue);
    }

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        IssueResponse created = issueService.createIssue(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable String id,
            @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long numericId = parseId(id);
        IssueResponse updated = issueService.updateIssue(numericId, request, principal.getId(), principal.getRole());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<IssueStatusHistoryResponse>> getIssueHistory(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long numericId = parseId(id);
        List<IssueStatusHistoryResponse> history = issueService.getIssueHistory(numericId, principal.getId(), principal.getRole());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<List<IssueEvidenceResponse>> getIssueEvidence(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long numericId = parseId(id);
        List<IssueEvidenceResponse> evidence = issueService.getIssueEvidence(numericId, principal.getId(), principal.getRole());
        return ResponseEntity.ok(evidence);
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<IssueEvidenceResponse> uploadEvidence(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long numericId = parseId(id);
        IssueEvidenceResponse response = issueEvidenceService.uploadEvidence(
                numericId,
                file,
                principal.getId(),
                principal.getRole()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long parseId(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("Issue ID cannot be blank");
        }
        String clean = id.trim().replaceFirst("(?i)^ISS-", "").replaceFirst("^#", "");
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid issue ID: " + id);
        }
    }
}
