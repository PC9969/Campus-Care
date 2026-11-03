package com.campuscare.backend.repository;

import com.campuscare.backend.entity.Issue;
import com.campuscare.backend.entity.IssuePriority;
import com.campuscare.backend.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByReportedBy(UUID reportedBy);

    List<Issue> findByReportedByOrderByCreatedAtDesc(UUID reportedBy);

    List<Issue> findByStatus(IssueStatus status);

    List<Issue> findAllByOrderByCreatedAtDesc();

    long countByStatus(IssueStatus status);

    long countByReportedBy(UUID reportedBy);

    long countByReportedByAndStatus(UUID reportedBy, IssueStatus status);

    long countByCategoryId(Long categoryId);

    long countByPriority(IssuePriority priority);
}
