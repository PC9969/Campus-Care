package com.campuscare.backend.repository;

import com.campuscare.backend.entity.ProfessorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfessorProfileRepository
        extends JpaRepository<ProfessorProfile, UUID> {
}