package com.campuscare.backend.repository;

import com.campuscare.backend.entity.IssueEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueEvidenceRepository extends JpaRepository<IssueEvidence, Long> {

    List<IssueEvidence> findByIssueId(Long issueId);

    List<IssueEvidence> findByIssueIdOrderByUploadedAtDesc(Long issueId);
}
