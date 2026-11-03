package com.campuscare.backend.repository;

import com.campuscare.backend.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, UUID> {
}