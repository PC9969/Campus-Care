package com.campuscare.backend.security;

import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.repository.ProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, profileRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMissingProfileForValidSupabaseSubjectGrantsNoAuthentication() throws Exception {
        UUID validSubUuid = UUID.randomUUID();
        String token = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(validSubUuid);

        // Profile does not exist in database for this subject
        when(profileRepository.findById(validSubUuid)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Security context must remain unauthenticated
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        // Verify no email fallback was attempted
        verify(profileRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void testExistingProfileForValidSupabaseSubjectAuthenticatesSuccessfully() throws Exception {
        UUID validSubUuid = UUID.randomUUID();
        String token = "valid.jwt.token";

        Profile profile = new Profile();
        profile.setId(validSubUuid);
        profile.setEmail("student@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(validSubUuid);
        when(profileRepository.findById(validSubUuid)).thenReturn(Optional.of(profile));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(validSubUuid, principal.getId());
        assertEquals("student@srmist.edu.in", principal.getUsername());
        verify(filterChain).doFilter(request, response);
    }
}
