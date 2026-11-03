package com.campuscare.backend.repository;

import com.campuscare.backend.entity.IssueAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueAssignmentRepository
        extends JpaRepository<IssueAssignment, Long> {

    List<IssueAssignment> findByStaffId(UUID staffId);

    List<IssueAssignment> findByIssueId(Long issueId);

    Optional<IssueAssignment> findTopByIssueIdOrderByAssignedAtDesc(Long issueId);

    List<IssueAssignment> findByStaffIdAndCompletedAtIsNull(UUID staffId);

    long countByStaffId(UUID staffId);

    long countByStaffIdAndCompletedAtIsNotNull(UUID staffId);

    long countByStaffIdAndCompletedAtIsNull(UUID staffId);
}
