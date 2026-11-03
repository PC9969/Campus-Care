package com.campuscare.backend.controller;

import com.campuscare.backend.dto.IssueEvidenceResponse;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.IssueEvidenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final IssueEvidenceService issueEvidenceService;

    public EvidenceController(IssueEvidenceService issueEvidenceService) {
        this.issueEvidenceService = issueEvidenceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueEvidenceResponse> getEvidenceById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        IssueEvidenceResponse response = issueEvidenceService.getEvidenceById(
                id,
                principal.getId(),
                principal.getRole()
        );
        return ResponseEntity.ok(response);
    }
}
