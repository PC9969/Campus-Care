package com.campuscare.backend.service;

import com.campuscare.backend.dto.StaffResponse;
import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.exception.ResourceNotFoundException;
import com.campuscare.backend.mapper.RoleMapper;
import com.campuscare.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProfessorProfileRepository professorProfileRepository;
    private final MaintenanceProfileRepository maintenanceProfileRepository;
    private final IssueRepository issueRepository;
    private final IssueAssignmentRepository issueAssignmentRepository;

    public ProfileService(
            ProfileRepository profileRepository,
            StudentProfileRepository studentProfileRepository,
            ProfessorProfileRepository professorProfileRepository,
            MaintenanceProfileRepository maintenanceProfileRepository,
            IssueRepository issueRepository,
            IssueAssignmentRepository issueAssignmentRepository
    ) {
        this.profileRepository = profileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.professorProfileRepository = professorProfileRepository;
        this.maintenanceProfileRepository = maintenanceProfileRepository;
        this.issueRepository = issueRepository;
        this.issueAssignmentRepository = issueAssignmentRepository;
    }

    public Profile getProfileById(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for ID: " + id));
    }

    public Optional<Profile> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return profileRepository.findByEmailIgnoreCase(email.trim());
    }

    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        Profile profile = getProfileById(userId);
        return buildUserProfileResponse(profile);
    }

    public List<UserProfileResponse> getAllUsers() {
        return profileRepository.findAll().stream()
                .map(this::buildUserProfileResponse)
                .toList();
    }

    public List<StaffResponse> getAllStaff() {
        List<Profile> staffProfiles = profileRepository.findByRole(UserRole.MAINTENANCE);
        List<StaffResponse> result = new ArrayList<>();

        for (Profile profile : staffProfiles) {
            StaffResponse staffDto = new StaffResponse();
            staffDto.setUserId(profile.getId());
            staffDto.setName(profile.getName());
            staffDto.setEmail(profile.getEmail());
            staffDto.setPhone(profile.getContactNumber() != null ? profile.getContactNumber() : "");

            Optional<MaintenanceProfile> mProfileOpt = maintenanceProfileRepository.findById(profile.getId());
            if (mProfileOpt.isPresent()) {
                MaintenanceProfile mProfile = mProfileOpt.get();
                staffDto.setId(mProfile.getStaffId());
                staffDto.setStaffId(mProfile.getStaffId());
                staffDto.setField(mProfile.getField());
            } else {
                staffDto.setId("STF-" + profile.getId().toString().substring(0, 6));
                staffDto.setStaffId(staffDto.getId());
                staffDto.setField("General Maintenance");
            }

            long totalAssigned = issueAssignmentRepository.countByStaffId(profile.getId());
            long totalSolved = issueAssignmentRepository.countByStaffIdAndCompletedAtIsNotNull(profile.getId());
            long activeTasks = issueAssignmentRepository.countByStaffIdAndCompletedAtIsNull(profile.getId());

            staffDto.setIssuesAssigned(totalAssigned);
            staffDto.setIssuesSolved(totalSolved);
            staffDto.setActiveIssues(activeTasks);

            if (activeTasks <= 1) {
                staffDto.setWorkload("Light");
            } else if (activeTasks <= 3) {
                staffDto.setWorkload("Moderate");
            } else {
                staffDto.setWorkload("High");
            }

            staffDto.setStatus(activeTasks > 0 ? "On Task" : "Available");
            result.add(staffDto);
        }

        return result;
    }

    public UserProfileResponse buildUserProfileResponse(Profile profile) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(profile.getId());
        resp.setName(profile.getName());
        resp.setEmail(profile.getEmail());
        resp.setContact(profile.getContactNumber() != null ? profile.getContactNumber() : "");
        resp.setRole(RoleMapper.toFrontendString(profile.getRole()));

        // Calculate dynamic issue metrics from actual DB data
        long reportedCount = issueRepository.countByReportedBy(profile.getId());
        long resolvedCount = issueRepository.countByReportedByAndStatus(profile.getId(), IssueStatus.RESOLVED)
                + issueRepository.countByReportedByAndStatus(profile.getId(), IssueStatus.CLOSED);

        resp.setIssuesReported(reportedCount);
        resp.setIssuesResolved(resolvedCount);

        switch (profile.getRole()) {
            case STUDENT -> {
                Optional<StudentProfile> studentOpt = studentProfileRepository.findById(profile.getId());
                if (studentOpt.isPresent()) {
                    resp.setBranch(studentOpt.get().getBranch());
                } else {
                    resp.setBranch("Computer Science & Engineering");
                }
                resp.setRegistrationNo("RA" + Math.abs(profile.getId().hashCode() % 10000000000L));
            }
            case PROFESSOR -> {
                Optional<ProfessorProfile> profOpt = professorProfileRepository.findById(profile.getId());
                if (profOpt.isPresent()) {
                    resp.setDepartment(profOpt.get().getDepartment());
                    resp.setDesignation(profOpt.get().getDesignation());
                } else {
                    resp.setDepartment("Academic Operations");
                    resp.setDesignation("Faculty Member");
                }
                resp.setFacultyId("FAC-SRM-" + Math.abs(profile.getId().hashCode() % 1000));
            }
            case MAINTENANCE -> {
                Optional<MaintenanceProfile> maintOpt = maintenanceProfileRepository.findById(profile.getId());
                if (maintOpt.isPresent()) {
                    resp.setField(maintOpt.get().getField());
                    resp.setStaffId(maintOpt.get().getStaffId());
                } else {
                    resp.setField("General Maintenance");
                    resp.setStaffId("STF-SRM-" + Math.abs(profile.getId().hashCode() % 1000));
                }
                long assignedCount = issueAssignmentRepository.countByStaffId(profile.getId());
                long solvedCount = issueAssignmentRepository.countByStaffIdAndCompletedAtIsNotNull(profile.getId());
                long activeTasks = issueAssignmentRepository.countByStaffIdAndCompletedAtIsNull(profile.getId());

                resp.setIssuesAssigned(assignedCount);
                resp.setIssuesSolved(solvedCount);
                resp.setActiveIssues(activeTasks);
                resp.setWorkload(activeTasks <= 1 ? "Light" : activeTasks <= 3 ? "Moderate" : "High");
                resp.setStatus(activeTasks > 0 ? "On Task" : "Available");
                resp.setPhone(profile.getContactNumber() != null ? profile.getContactNumber() : "");
            }
            case ADMIN -> {
                resp.setDesignation("Dean & Director of Campus Operations");
                resp.setDepartment("Office of Campus Administration");
            }
        }

        return resp;
    }
}
