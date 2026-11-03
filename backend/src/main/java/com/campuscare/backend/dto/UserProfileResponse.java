package com.campuscare.backend.dto;

import java.util.UUID;

public class UserProfileResponse {

    private UUID id;
    private String name;
    private String email;
    private String contact;
    private String role; // Frontend string: Student, Professor, Maintenance Staff, Admin

    // Student specific
    private String branch;
    private String registrationNo;

    // Professor specific
    private String department;
    private String designation;
    private String facultyId;

    // Maintenance specific
    private String field;
    private String staffId;
    private Long issuesAssigned;
    private Long issuesSolved;
    private Long activeIssues;
    private String workload; // Light, Moderate, High
    private String status;   // Available, On Task
    private String phone;

    // Common dynamic metrics
    private Long issuesReported;
    private Long issuesResolved;

    public UserProfileResponse() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
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

    public Long getIssuesAssigned() {
        return issuesAssigned;
    }

    public void setIssuesAssigned(Long issuesAssigned) {
        this.issuesAssigned = issuesAssigned;
    }

    public Long getIssuesSolved() {
        return issuesSolved;
    }

    public void setIssuesSolved(Long issuesSolved) {
        this.issuesSolved = issuesSolved;
    }

    public Long getActiveIssues() {
        return activeIssues;
    }

    public void setActiveIssues(Long activeIssues) {
        this.activeIssues = activeIssues;
    }

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getIssuesReported() {
        return issuesReported;
    }

    public void setIssuesReported(Long issuesReported) {
        this.issuesReported = issuesReported;
    }

    public Long getIssuesResolved() {
        return issuesResolved;
    }

    public void setIssuesResolved(Long issuesResolved) {
        this.issuesResolved = issuesResolved;
    }
}
