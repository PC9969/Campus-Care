package com.campuscare.backend.repository;

import com.campuscare.backend.entity.IssueCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueCategoryRepository extends JpaRepository<IssueCategory, Long> {

    Optional<IssueCategory> findByName(String name);

    Optional<IssueCategory> findByNameIgnoreCase(String name);
}
