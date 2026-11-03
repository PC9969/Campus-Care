package com.campuscare.backend.dto;

import java.util.UUID;

public class StaffResponse {

    private String id; // e.g. "STF-01" or staffId
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private String field;
    private String staffId;
    private String workload; // Light, Moderate, High
    private long activeIssues;
    private long issuesAssigned;
    private long issuesSolved;
    private String status; // Available, On Task

    public StaffResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public long getActiveIssues() {
        return activeIssues;
    }

    public void setActiveIssues(long activeIssues) {
        this.activeIssues = activeIssues;
    }

    public long getIssuesAssigned() {
        return issuesAssigned;
    }

    public void setIssuesAssigned(long issuesAssigned) {
        this.issuesAssigned = issuesAssigned;
    }

    public long getIssuesSolved() {
        return issuesSolved;
    }

    public void setIssuesSolved(long issuesSolved) {
        this.issuesSolved = issuesSolved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
