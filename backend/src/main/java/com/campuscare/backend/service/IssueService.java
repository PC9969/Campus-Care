package com.campuscare.backend.service;

import com.campuscare.backend.dto.*;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.exception.InvalidWorkflowTransitionException;
import com.campuscare.backend.exception.ResourceNotFoundException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.mapper.PriorityMapper;
import com.campuscare.backend.mapper.RoleMapper;
import com.campuscare.backend.mapper.StatusMapper;
import com.campuscare.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueCategoryRepository issueCategoryRepository;
    private final IssueAssignmentRepository issueAssignmentRepository;
    private final IssueEvidenceRepository issueEvidenceRepository;
    private final IssueStatusHistoryRepository issueStatusHistoryRepository;
    private final ProfileRepository profileRepository;
    private final IssueCategoryService issueCategoryService;
    private final IssueAssignmentService issueAssignmentService;
    private final IssueEvidenceService issueEvidenceService;

    public IssueService(
            IssueRepository issueRepository,
            IssueCategoryRepository issueCategoryRepository,
            IssueAssignmentRepository issueAssignmentRepository,
            IssueEvidenceRepository issueEvidenceRepository,
            IssueStatusHistoryRepository issueStatusHistoryRepository,
            ProfileRepository profileRepository,
            IssueCategoryService issueCategoryService,
            IssueAssignmentService issueAssignmentService,
            IssueEvidenceService issueEvidenceService
    ) {
        this.issueRepository = issueRepository;
        this.issueCategoryRepository = issueCategoryRepository;
        this.issueAssignmentRepository = issueAssignmentRepository;
        this.issueEvidenceRepository = issueEvidenceRepository;
        this.issueStatusHistoryRepository = issueStatusHistoryRepository;
        this.profileRepository = profileRepository;
        this.issueCategoryService = issueCategoryService;
        this.issueAssignmentService = issueAssignmentService;
        this.issueEvidenceService = issueEvidenceService;
    }

    public IssueResponse createIssue(CreateIssueRequest request, UUID reporterId) {
        if (reporterId == null) {
            throw new UnauthorizedException("User must be authenticated to report an issue");
        }

        Profile reporter = profileRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter profile not found"));

        IssueCategory category = issueCategoryService.findOrCreateCategory(request.getCategory());

        IssuePriority priority = PriorityMapper.fromFrontendString(request.getPriority());

        OffsetDateTime now = OffsetDateTime.now();

        Issue issue = new Issue();
        issue.setReportedBy(reporterId);
        issue.setTitle(request.getTitle().trim());
        issue.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        issue.setCategory(category);
        issue.setLocation(request.getLocation().trim());
        issue.setPriority(priority);
        issue.setStatus(IssueStatus.OPEN);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);

        Issue saved = issueRepository.save(issue);

        // Record initial status history
        recordStatusHistory(saved, IssueStatus.OPEN, reporterId, "Issue reported", now);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueById(Long id, UUID currentUserId, UserRole role) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + id));

        verifyReadAccess(issue, currentUserId, role);

        return mapToResponse(issue);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getAllIssues(
            UUID currentUserId,
            UserRole role,
            String statusFilter,
            String categoryFilter,
            String priorityFilter,
            String search
    ) {
        List<Issue> issues;

        if (role == UserRole.STUDENT || role == UserRole.PROFESSOR) {
            // Students and Professors only view their own issues
            issues = issueRepository.findByReportedByOrderByCreatedAtDesc(currentUserId);
        } else {
            // Maintenance staff and Admin can view the campus triage queue
            issues = issueRepository.findAllByOrderByCreatedAtDesc();
        }

        return filterIssues(issues, statusFilter, categoryFilter, priorityFilter, search).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getMyIssues(UUID currentUserId) {
        return issueRepository.findByReportedByOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public IssueResponse updateIssue(Long id, UpdateIssueRequest request, UUID currentUserId, UserRole role) {
        Issue targetIssue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + id));

        final Long targetIssueId = targetIssue.getId();

        // Permission checks
        if (role == UserRole.STUDENT || role == UserRole.PROFESSOR) {
            if (!targetIssue.getReportedBy().equals(currentUserId)) {
                throw new UnauthorizedException("You can only modify your own reported issues");
            }
            if (targetIssue.getStatus() != IssueStatus.OPEN) {
                throw new BadRequestException("Cannot edit an issue once work has commenced or resolved");
            }
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                targetIssue.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null) {
                targetIssue.setDescription(request.getDescription().trim());
            }
            if (request.getLocation() != null && !request.getLocation().isBlank()) {
                targetIssue.setLocation(request.getLocation().trim());
            }
            if (request.getCategory() != null && !request.getCategory().isBlank()) {
                targetIssue.setCategory(issueCategoryService.findOrCreateCategory(request.getCategory()));
            }
            targetIssue.setUpdatedAt(OffsetDateTime.now());
            return mapToResponse(issueRepository.save(targetIssue));
        }

        // Admin or Maintenance updates
        if (role == UserRole.MAINTENANCE) {
            boolean isAssigned = issueAssignmentRepository.findByStaffId(currentUserId).stream()
                    .anyMatch(a -> a.getIssue().getId().equals(targetIssueId));
            if (!isAssigned && role != UserRole.ADMIN) {
                throw new UnauthorizedException("Maintenance staff can only update issues assigned to them");
            }
        }

        // Apply status update if provided
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            IssueStatus targetStatus = StatusMapper.fromFrontendString(request.getStatus());
            if (targetStatus != targetIssue.getStatus()) {
                updateIssueStatus(id, targetStatus, request.getComment(), currentUserId, role);
                targetIssue = issueRepository.findById(id).orElseThrow();
            }
        }

        // Admin can update assignee, priority, category, location, title
        if (role == UserRole.ADMIN) {
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                targetIssue.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null) {
                targetIssue.setDescription(request.getDescription().trim());
            }
            if (request.getLocation() != null && !request.getLocation().isBlank()) {
                targetIssue.setLocation(request.getLocation().trim());
            }
            if (request.getCategory() != null && !request.getCategory().isBlank()) {
                targetIssue.setCategory(issueCategoryService.findOrCreateCategory(request.getCategory()));
            }
            if (request.getPriority() != null && !request.getPriority().isBlank()) {
                targetIssue.setPriority(PriorityMapper.fromFrontendString(request.getPriority()));
            }
            if (request.getAssignee() != null && !request.getAssignee().isBlank()) {
                handleAssigneeUpdate(targetIssue, request.getAssignee(), currentUserId);
            }
            targetIssue.setUpdatedAt(OffsetDateTime.now());
            targetIssue = issueRepository.save(targetIssue);
        }

        return mapToResponse(targetIssue);
    }

    public IssueResponse updateIssueStatus(Long id, IssueStatus newStatus, String comment, UUID currentUserId, UserRole role) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + id));

        if (issue.getStatus() == newStatus) {
            return mapToResponse(issue);
        }

        validateStatusTransition(issue.getStatus(), newStatus, role);

        // Maintenance permission check
        if (role == UserRole.MAINTENANCE) {
            boolean isAssigned = issueAssignmentRepository.findByStaffId(currentUserId).stream()
                    .anyMatch(a -> a.getIssue().getId().equals(issue.getId()));
            if (!isAssigned) {
                throw new UnauthorizedException("You can only change status for issues assigned to you");
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        issue.setStatus(newStatus);
        issue.setUpdatedAt(now);

        if (newStatus == IssueStatus.RESOLVED) {
            issue.setResolvedAt(now);
            issueAssignmentService.completeAssignment(id);
        } else if (newStatus == IssueStatus.OPEN) {
            issue.setResolvedAt(null);
        }

        Issue saved = issueRepository.save(issue);
        // Supabase DB trigger automatically records status updates into issue_status_history
        return mapToResponse(saved);
    }

    public IssueResponse resolveIssue(Long id, String comment, UUID currentUserId, UserRole role) {
        return updateIssueStatus(id, IssueStatus.RESOLVED, comment, currentUserId, role);
    }

    public IssueResponse closeIssue(Long id, String comment, UUID currentUserId, UserRole role) {
        return updateIssueStatus(id, IssueStatus.CLOSED, comment, currentUserId, role);
    }

    public IssueResponse assignIssue(Long id, UUID staffId, UUID assignedBy, UserRole callerRole) {
        if (callerRole != UserRole.ADMIN) {
            throw new UnauthorizedException("Only administrators can assign tickets");
        }

        issueAssignmentService.assignStaff(id, staffId, assignedBy);
        Issue issue = issueRepository.findById(id).orElseThrow();
        return mapToResponse(issue);
    }

    @Transactional(readOnly = true)
    public List<IssueStatusHistoryResponse> getIssueHistory(Long id, UUID currentUserId, UserRole role) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + id));

        verifyReadAccess(issue, currentUserId, role);

        return issueStatusHistoryRepository.findByIssueIdOrderByChangedAtAsc(id).stream()
                .map(h -> {
                    IssueStatusHistoryResponse resp = new IssueStatusHistoryResponse();
                    resp.setId(h.getId());
                    resp.setIssueId(h.getIssue().getId());
                    resp.setStatus(StatusMapper.toFrontendString(h.getStatus()));
                    resp.setChangedBy(h.getChangedBy());
                    if (h.getChangedBy() != null) {
                        resp.setChangedByName(profileRepository.findById(h.getChangedBy())
                                .map(Profile::getName)
                                .orElse("System / User"));
                    }
                    resp.setComment(h.getComment());
                    resp.setChangedAt(h.getChangedAt());
                    return resp;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IssueEvidenceResponse> getIssueEvidence(Long id, UUID currentUserId, UserRole role) {
        return issueEvidenceService.getEvidenceByIssue(id, currentUserId, role);
    }

    @Transactional(readOnly = true)
    public IssueSummaryResponse getSummary(UUID currentUserId, UserRole role) {
        List<Issue> allIssues = issueRepository.findAll();

        long open = allIssues.stream().filter(i -> i.getStatus() == IssueStatus.OPEN).count();
        long inProgress = allIssues.stream().filter(i -> i.getStatus() == IssueStatus.IN_PROGRESS).count();
        long resolved = allIssues.stream().filter(i -> i.getStatus() == IssueStatus.RESOLVED).count();
        long closed = allIssues.stream().filter(i -> i.getStatus() == IssueStatus.CLOSED).count();
        long total = allIssues.size();

        OffsetDateTime oneWeekAgo = OffsetDateTime.now().minusDays(7);
        long newThisWeek = allIssues.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(oneWeekAgo))
                .count();

        // Calculate average resolution duration
        double totalResolutionHours = 0;
        int resolvedCount = 0;
        for (Issue issue : allIssues) {
            if (issue.getResolvedAt() != null && issue.getCreatedAt() != null) {
                Duration duration = Duration.between(issue.getCreatedAt(), issue.getResolvedAt());
                if (!duration.isNegative()) {
                    totalResolutionHours += duration.toHours();
                    resolvedCount++;
                }
            }
        }
        String avgResolution = resolvedCount > 0
                ? String.format(Locale.US, "%.1f days", (totalResolutionHours / resolvedCount) / 24.0)
                : "1.8 days";

        // Categories breakdown
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Issue issue : allIssues) {
            if (issue.getStatus() != IssueStatus.RESOLVED && issue.getStatus() != IssueStatus.CLOSED) {
                String catName = issue.getCategory() != null ? issue.getCategory().getName() : "General";
                categoryCounts.put(catName, categoryCounts.getOrDefault(catName, 0L) + 1);
            }
        }

        List<CategoryCountDto> categories = categoryCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> new CategoryCountDto(e.getKey(), e.getValue()))
                .toList();

        // Priority breakdown
        Map<String, Long> priorityCounts = new HashMap<>();
        for (Issue issue : allIssues) {
            String pStr = PriorityMapper.toFrontendString(issue.getPriority());
            priorityCounts.put(pStr, priorityCounts.getOrDefault(pStr, 0L) + 1);
        }
        List<PriorityCountDto> priorities = priorityCounts.entrySet().stream()
                .map(e -> new PriorityCountDto(e.getKey(), e.getValue()))
                .toList();

        // Weekly trends breakdown
        List<WeeklyTrendDto> weekly = new ArrayList<>();
        OffsetDateTime today = OffsetDateTime.now();
        for (int i = 6; i >= 0; i--) {
            OffsetDateTime dayStart = today.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime dayEnd = dayStart.plusDays(1);
            String dayName = dayStart.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            long reported = allIssues.stream()
                    .filter(issue -> issue.getCreatedAt() != null
                            && !issue.getCreatedAt().isBefore(dayStart)
                            && issue.getCreatedAt().isBefore(dayEnd))
                    .count();

            long res = allIssues.stream()
                    .filter(issue -> issue.getResolvedAt() != null
                            && !issue.getResolvedAt().isBefore(dayStart)
                            && issue.getResolvedAt().isBefore(dayEnd))
                    .count();

            weekly.add(new WeeklyTrendDto(dayName, reported, res));
        }

        IssueSummaryResponse summary = new IssueSummaryResponse();
        summary.setOpen(open);
        summary.setInProgress(inProgress);
        summary.setResolved(resolved);
        summary.setClosed(closed);
        summary.setTotal(total);
        summary.setNewThisWeek(newThisWeek);
        summary.setAvgResolution(avgResolution);
        summary.setCategories(categories);
        summary.setPriorities(priorities);
        summary.setWeekly(weekly);

        return summary;
    }

    private void validateStatusTransition(IssueStatus current, IssueStatus next, UserRole role) {
        if (current == next) return;

        // Admin can perform administrative corrections
        if (role == UserRole.ADMIN) {
            return;
        }

        // Standard workflow: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
        boolean valid = switch (current) {
            case OPEN -> next == IssueStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == IssueStatus.RESOLVED;
            case RESOLVED -> next == IssueStatus.CLOSED;
            case CLOSED -> false;
        };

        if (!valid) {
            throw new InvalidWorkflowTransitionException(
                    "Invalid status transition from " + StatusMapper.toFrontendString(current)
                            + " to " + StatusMapper.toFrontendString(next)
            );
        }
    }

    private void recordStatusHistory(Issue issue, IssueStatus status, UUID changedBy, String comment, OffsetDateTime changedAt) {
        IssueStatusHistory history = new IssueStatusHistory();
        history.setIssue(issue);
        history.setStatus(status);
        history.setChangedBy(changedBy);
        history.setComment(comment);
        history.setChangedAt(changedAt);
        issueStatusHistoryRepository.save(history);
    }

    private void verifyReadAccess(Issue issue, UUID currentUserId, UserRole role) {
        if (role == UserRole.ADMIN || role == UserRole.MAINTENANCE) {
            return;
        }
        if (!issue.getReportedBy().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to view this ticket");
        }
    }

    private void handleAssigneeUpdate(Issue issue, String assigneeStr, UUID assignedBy) {
        // Try looking up by name or staffId or UUID
        try {
            UUID staffUuid = UUID.fromString(assigneeStr);
            issueAssignmentService.assignStaff(issue.getId(), staffUuid, assignedBy);
            return;
        } catch (IllegalArgumentException ignored) {}

        List<Profile> staffProfiles = profileRepository.findByRole(UserRole.MAINTENANCE);
        for (Profile p : staffProfiles) {
            if (p.getName().equalsIgnoreCase(assigneeStr.trim())) {
                issueAssignmentService.assignStaff(issue.getId(), p.getId(), assignedBy);
                return;
            }
        }
    }

    private List<Issue> filterIssues(List<Issue> issues, String status, String category, String priority, String search) {
        return issues.stream()
                .filter(i -> {
                    if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All issues")) {
                        IssueStatus target = StatusMapper.fromFrontendString(status);
                        return i.getStatus() == target;
                    }
                    return true;
                })
                .filter(i -> {
                    if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All categories")) {
                        return i.getCategory() != null && i.getCategory().getName().equalsIgnoreCase(category);
                    }
                    return true;
                })
                .filter(i -> {
                    if (priority != null && !priority.isBlank() && !priority.equalsIgnoreCase("All priorities")) {
                        IssuePriority target = PriorityMapper.fromFrontendString(priority);
                        return i.getPriority() == target;
                    }
                    return true;
                })
                .filter(i -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        String idStr = "iss-" + i.getId();
                        boolean matchId = String.valueOf(i.getId()).contains(s) || idStr.contains(s);
                        boolean matchTitle = i.getTitle().toLowerCase().contains(s);
                        boolean matchLocation = i.getLocation().toLowerCase().contains(s);
                        return matchId || matchTitle || matchLocation;
                    }
                    return true;
                })
                .toList();
    }

    public IssueResponse mapToResponse(Issue issue) {
        IssueResponse resp = new IssueResponse();
        resp.setId("ISS-" + issue.getId());
        resp.setNumericId(issue.getId());
        resp.setTitle(issue.getTitle());
        resp.setDescription(issue.getDescription());

        if (issue.getCategory() != null) {
            resp.setCategory(issue.getCategory().getName());
            resp.setCategoryId(issue.getCategory().getId());
        } else {
            resp.setCategory("General Maintenance");
        }

        resp.setLocation(issue.getLocation());
        resp.setPriority(PriorityMapper.toFrontendString(issue.getPriority()));
        resp.setStatus(StatusMapper.toFrontendString(issue.getStatus()));
        resp.setCreatedAt(issue.getCreatedAt());
        resp.setUpdatedAt(issue.getUpdatedAt());
        resp.setResolvedAt(issue.getResolvedAt());
        resp.setReportedBy(issue.getReportedBy());

        // Populate reporter details
        profileRepository.findById(issue.getReportedBy()).ifPresent(p -> {
            resp.setReportedByName(p.getName());
            resp.setReportedByRole(RoleMapper.toFrontendString(p.getRole()));
        });

        // Populate current assignee details
        Optional<IssueAssignment> activeAssign = issueAssignmentRepository.findTopByIssueIdOrderByAssignedAtDesc(issue.getId());
        if (activeAssign.isPresent()) {
            IssueAssignment assign = activeAssign.get();
            resp.setAssigneeId(assign.getStaffId());
            profileRepository.findById(assign.getStaffId()).ifPresent(p -> resp.setAssignee(p.getName()));
        } else {
            resp.setAssignee("Unassigned");
        }

        // Attach first evidence if available
        List<IssueEvidence> evidences = issueEvidenceRepository.findByIssueIdOrderByUploadedAtDesc(issue.getId());
        if (!evidences.isEmpty()) {
            resp.setEvidence(issueEvidenceService.mapToResponse(evidences.get(0)));
        }

        return resp;
    }
}
