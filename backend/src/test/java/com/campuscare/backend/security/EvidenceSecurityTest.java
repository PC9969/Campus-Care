package com.campuscare.backend.security;

import com.campuscare.backend.controller.EvidenceController;
import com.campuscare.backend.repository.ProfileRepository;
import com.campuscare.backend.service.IssueEvidenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EvidenceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class EvidenceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ProfileRepository profileRepository;

    @MockitoBean
    private IssueEvidenceService issueEvidenceService;

    @Test
    void testEvidenceEndpointsRequireAuthentication() throws Exception {
        // Without authentication, access to /api/evidence/{id} must be rejected with 401 Unauthorized
        mockMvc.perform(get("/api/evidence/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRemovedEvidenceViewEndpointIsNoLongerPermittedWithoutAuth() throws Exception {
        // /api/evidence/view was previously permitAll; it must now require authentication and return 401
        mockMvc.perform(get("/api/evidence/view?path=issue-1/test.jpg"))
                .andExpect(status().isUnauthorized());
    }
}
