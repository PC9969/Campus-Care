package com.campuscare.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "professor_profiles")
public class ProfessorProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String department;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}