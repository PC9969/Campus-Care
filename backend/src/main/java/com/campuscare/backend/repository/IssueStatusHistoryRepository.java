package com.campuscare.backend.repository;

import com.campuscare.backend.entity.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueStatusHistoryRepository
        extends JpaRepository<IssueStatusHistory, Long> {

    List<IssueStatusHistory> findByIssueIdOrderByChangedAtAsc(Long issueId);
}