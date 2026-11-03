package com.campuscare.backend.config;

import com.campuscare.backend.entity.*;
import com.campuscare.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Configuration
@Profile("local")
public class LocalDataInitializer {

    @Bean
    public CommandLineRunner initLocalData(
            ProfileRepository profileRepository,
            StudentProfileRepository studentProfileRepository,
            ProfessorProfileRepository professorProfileRepository,
            MaintenanceProfileRepository maintenanceProfileRepository,
            IssueCategoryRepository issueCategoryRepository,
            IssueRepository issueRepository,
            IssueAssignmentRepository issueAssignmentRepository,
            IssueStatusHistoryRepository issueStatusHistoryRepository
    ) {
        return args -> {
            if (profileRepository.count() > 0) return;

            // 1. Categories
            List<String> categoryNames = List.of(
                    "Plumbing", "Electrical", "HVAC", "Carpentry",
                    "Networking", "Cleaning", "Furniture", "Grounds", "Safety", "General Maintenance"
            );
            IssueCategory plumbing = null;
            IssueCategory electrical = null;
            for (String catName : categoryNames) {
                IssueCategory cat = new IssueCategory();
                cat.setName(catName);
                cat.setDescription(catName + " maintenance category");
                IssueCategory saved = issueCategoryRepository.save(cat);
                if ("Plumbing".equals(catName)) plumbing = saved;
                if ("Electrical".equals(catName)) electrical = saved;
            }

            OffsetDateTime now = OffsetDateTime.now();

            // 2. Student (Rahul Sharma)
            UUID studentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            com.campuscare.backend.entity.Profile student = new com.campuscare.backend.entity.Profile();
            student.setId(studentId);
            student.setName("Rahul Sharma");
            student.setEmail("rahul.s@srmist.edu.in");
            student.setContactNumber("+91 98765 43210");
            student.setRole(UserRole.STUDENT);
            student.setCreatedAt(now);
            student.setUpdatedAt(now);
            profileRepository.save(student);

            StudentProfile sp = new StudentProfile();
            sp.setUserId(studentId);
            sp.setBranch("Computer Science & Engineering");
            studentProfileRepository.save(sp);

            // 3. Professor (Dr. Priya Nair)
            UUID profId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            com.campuscare.backend.entity.Profile prof = new com.campuscare.backend.entity.Profile();
            prof.setId(profId);
            prof.setName("Dr. Priya Nair");
            prof.setEmail("priya.nair@srmist.edu.in");
            prof.setContactNumber("+91 98111 22334");
            prof.setRole(UserRole.PROFESSOR);
            prof.setCreatedAt(now);
            prof.setUpdatedAt(now);
            profileRepository.save(prof);

            ProfessorProfile pp = new ProfessorProfile();
            pp.setUserId(profId);
            pp.setDepartment("Department of Mechanical Engineering");
            pp.setDesignation("Associate Professor");
            professorProfileRepository.save(pp);

            // 4. Maintenance Staff (Amit Kumar)
            UUID staffId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            com.campuscare.backend.entity.Profile staff = new com.campuscare.backend.entity.Profile();
            staff.setId(staffId);
            staff.setName("Amit Kumar");
            staff.setEmail("amit.k@srmist.edu.in");
            staff.setContactNumber("+91 97234 56789");
            staff.setRole(UserRole.MAINTENANCE);
            staff.setCreatedAt(now);
            staff.setUpdatedAt(now);
            profileRepository.save(staff);

            MaintenanceProfile mp = new MaintenanceProfile();
            mp.setUserId(staffId);
            mp.setStaffId("STF-SRM-019");
            mp.setField("Electrical");
            maintenanceProfileRepository.save(mp);

            // 5. Admin (Dr. S. K. Gupta)
            UUID adminId = UUID.fromString("44444444-4444-4444-4444-444444444444");
            com.campuscare.backend.entity.Profile admin = new com.campuscare.backend.entity.Profile();
            admin.setId(adminId);
            admin.setName("Dr. S. K. Gupta");
            admin.setEmail("dean.admin@srmist.edu.in");
            admin.setContactNumber("+91 98000 11223");
            admin.setRole(UserRole.ADMIN);
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);
            profileRepository.save(admin);

            // 6. Seed an initial issue
            Issue issue1 = new Issue();
            issue1.setReportedBy(studentId);
            issue1.setTitle("Water leakage near Fluids Lab");
            issue1.setDescription("Water is pooling continuously near the washroom entrance adjacent to the lab.");
            issue1.setCategory(plumbing);
            issue1.setLocation("Mechanical Block · Ground Floor, Fluids Lab");
            issue1.setPriority(IssuePriority.HIGH);
            issue1.setStatus(IssueStatus.IN_PROGRESS);
            issue1.setCreatedAt(now.minusDays(2));
            issue1.setUpdatedAt(now.minusDays(1));
            Issue savedIssue1 = issueRepository.save(issue1);

            IssueAssignment assign1 = new IssueAssignment();
            assign1.setIssue(savedIssue1);
            assign1.setStaffId(staffId);
            assign1.setAssignedBy(adminId);
            assign1.setAssignedAt(now.minusDays(1));
            issueAssignmentRepository.save(assign1);

            IssueStatusHistory hist1 = new IssueStatusHistory();
            hist1.setIssue(savedIssue1);
            hist1.setStatus(IssueStatus.OPEN);
            hist1.setChangedBy(studentId);
            hist1.setComment("Issue reported");
            hist1.setChangedAt(now.minusDays(2));
            issueStatusHistoryRepository.save(hist1);

            IssueStatusHistory hist2 = new IssueStatusHistory();
            hist2.setIssue(savedIssue1);
            hist2.setStatus(IssueStatus.IN_PROGRESS);
            hist2.setChangedBy(adminId);
            hist2.setComment("Assigned to Amit Kumar");
            hist2.setChangedAt(now.minusDays(1));
            issueStatusHistoryRepository.save(hist2);
        };
    }
}
