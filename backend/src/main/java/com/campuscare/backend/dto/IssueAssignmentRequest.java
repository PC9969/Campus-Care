package com.campuscare.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class IssueAssignmentRequest {

    @NotNull(message = "Issue ID is required")
    private Long issueId;

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    public IssueAssignmentRequest() {}

    public IssueAssignmentRequest(Long issueId, UUID staffId) {
        this.issueId = issueId;
        this.staffId = staffId;
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public void setStaffId(UUID staffId) {
        this.staffId = staffId;
    }
}
