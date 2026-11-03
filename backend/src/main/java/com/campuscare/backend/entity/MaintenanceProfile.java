package com.campuscare.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "maintenance_profiles")
public class MaintenanceProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "staff_id", nullable = false, unique = true)
    private String staffId;

    @Column(nullable = false)
    private String field;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}