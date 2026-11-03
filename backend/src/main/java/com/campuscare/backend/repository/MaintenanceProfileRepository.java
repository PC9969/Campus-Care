package com.campuscare.backend.repository;

import com.campuscare.backend.entity.MaintenanceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MaintenanceProfileRepository
        extends JpaRepository<MaintenanceProfile, UUID> {
}