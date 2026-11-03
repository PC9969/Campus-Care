package com.campuscare.backend.repository;

import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByEmailIgnoreCase(String email);

    List<Profile> findByRole(UserRole role);
}
