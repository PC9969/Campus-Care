package com.campuscare.backend.controller;

import com.campuscare.backend.dto.CategoryResponse;
import com.campuscare.backend.dto.IssueResponse;
import com.campuscare.backend.dto.IssueSummaryResponse;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.security.JwtAuthenticationEntryPoint;
import com.campuscare.backend.security.JwtAuthenticationFilter;
import com.campuscare.backend.service.IssueCategoryService;
import com.campuscare.backend.service.IssueEvidenceService;
import com.campuscare.backend.service.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IssueController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private IssueCategoryService issueCategoryService;

    @MockitoBean
    private IssueEvidenceService issueEvidenceService;

    private CustomUserPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        com.campuscare.backend.entity.Profile p = new com.campuscare.backend.entity.Profile();
        p.setId(UUID.randomUUID());
        p.setName("Rahul Sharma");
        p.setEmail("rahul.s@srmist.edu.in");
        p.setRole(UserRole.STUDENT);
        studentPrincipal = new CustomUserPrincipal(p);
    }

    @Test
    void testGetCategories() throws Exception {
        when(issueCategoryService.getAllCategories()).thenReturn(List.of(
                new CategoryResponse(1L, "Plumbing", "Plumbing issues", 3),
                new CategoryResponse(2L, "Electrical", "Electrical issues", 5)
        ));

        mockMvc.perform(get("/api/issues/categories").with(user(studentPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plumbing"))
                .andExpect(jsonPath("$[0].count").value(3))
                .andExpect(jsonPath("$[1].name").value("Electrical"));
    }

    @Test
    void testGetSummary() throws Exception {
        IssueSummaryResponse summary = new IssueSummaryResponse();
        summary.setOpen(5);
        summary.setInProgress(3);
        summary.setResolved(10);
        summary.setClosed(4);
        summary.setTotal(22);

        when(issueService.getSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/issues/summary").with(user(studentPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(5))
                .andExpect(jsonPath("$.inProgress").value(3))
                .andExpect(jsonPath("$.resolved").value(10))
                .andExpect(jsonPath("$.total").value(22));
    }

    @Test
    void testCreateIssueValidationFailure() throws Exception {
        // Missing title and location
        String invalidJson = "{\"category\":\"Plumbing\",\"priority\":\"Medium\"}";

        mockMvc.perform(post("/api/issues")
                        .with(user(studentPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
