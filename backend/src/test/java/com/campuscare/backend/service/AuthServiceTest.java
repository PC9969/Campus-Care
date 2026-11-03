package com.campuscare.backend.service;

import com.campuscare.backend.dto.LoginRequest;
import com.campuscare.backend.dto.LoginResponse;
import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.ProfileRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileService profileService;

    private AuthService authService;
    private HttpServer mockSupabaseServer;
    private String mockSupabaseUrl;
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = new AuthService(profileRepository, profileService);
        lastRequestBody.set(null);
    }

    @AfterEach
    void tearDown() {
        if (mockSupabaseServer != null) {
            mockSupabaseServer.stop(0);
        }
    }

    @Test
    void testLoginFailsSecurelyWhenSupabaseNotConfigured() {
        authService.setSupabaseConfig("", "");

        LoginRequest req = new LoginRequest("student@srmist.edu.in", "password123", "Student / Professor");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("not configured"));
        verifyNoInteractions(profileRepository);
    }

    @Test
    void testLoginFailsWhenSupabaseReturnsInvalidCredentials() throws Exception {
        startMockSupabaseServer(400, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid login credentials\"}");
        authService.setSupabaseConfig(mockSupabaseUrl, "test-anon-key");

        LoginRequest req = new LoginRequest("student@srmist.edu.in", "wrong-password", "Student / Professor");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(req));
        assertEquals("Invalid credentials. Please verify your email and password.", ex.getMessage());
        verifyNoInteractions(profileRepository);
    }

    @Test
    void testLoginFailsWhenSupabaseReturns500ServerError() throws Exception {
        startMockSupabaseServer(500, "{\"error\":\"internal_server_error\"}");
        authService.setSupabaseConfig(mockSupabaseUrl, "test-anon-key");

        LoginRequest req = new LoginRequest("student@srmist.edu.in", "password123", "Student / Professor");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("Supabase Auth error"));
        verifyNoInteractions(profileRepository);
    }

    @Test
    void testLoginFailsWhenSupabaseIsUnreachable() {
        authService.setSupabaseConfig("http://127.0.0.1:59999", "test-anon-key");

        LoginRequest req = new LoginRequest("student@srmist.edu.in", "password123", "Student / Professor");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("unreachable") || ex.getMessage().contains("Authentication error"));
        verifyNoInteractions(profileRepository);
    }

    @Test
    void testLoginSuccessWhenSupabaseVerifiesCredentials() throws Exception {
        String tokenPayload = "{\"access_token\":\"supabase-valid-session-jwt-token\",\"token_type\":\"bearer\"}";
        startMockSupabaseServer(200, tokenPayload);
        authService.setSupabaseConfig(mockSupabaseUrl, "test-anon-key");

        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(userId);
        profile.setEmail("student@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(userId);
        userProfileResponse.setEmail("student@srmist.edu.in");
        userProfileResponse.setRole("Student");

        when(profileRepository.findByEmailIgnoreCase("student@srmist.edu.in")).thenReturn(Optional.of(profile));
        when(profileService.buildUserProfileResponse(profile)).thenReturn(userProfileResponse);

        LoginRequest req = new LoginRequest("student@srmist.edu.in", "correct-password", "Student / Professor");
        LoginResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("supabase-valid-session-jwt-token", response.getToken());
        assertEquals("student@srmist.edu.in", response.getUser().getEmail());
        assertEquals("Student", response.getUser().getRole());
    }

    @Test
    void testPasswordWithLeadingAndTrailingSpacesIsPreservedUntrimmed() throws Exception {
        String tokenPayload = "{\"access_token\":\"valid-token\",\"token_type\":\"bearer\"}";
        startMockSupabaseServer(200, tokenPayload);
        authService.setSupabaseConfig(mockSupabaseUrl, "test-anon-key");

        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(userId);
        profile.setEmail("student@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(userId);
        userProfileResponse.setEmail("student@srmist.edu.in");
        userProfileResponse.setRole("Student");

        when(profileRepository.findByEmailIgnoreCase("student@srmist.edu.in")).thenReturn(Optional.of(profile));
        when(profileService.buildUserProfileResponse(profile)).thenReturn(userProfileResponse);

        String untrimmedPassword = "   my secret password with spaces   ";
        LoginRequest req = new LoginRequest("  STUDENT@srmist.edu.in  ", untrimmedPassword, "Student / Professor");

        LoginResponse response = authService.login(req);
        assertNotNull(response);

        // Verify request payload sent to Supabase Auth
        assertNotNull(lastRequestBody.get());
        JsonNode receivedJson = objectMapper.readTree(lastRequestBody.get());
        assertEquals(untrimmedPassword, receivedJson.get("password").asText());
        assertEquals("student@srmist.edu.in", receivedJson.get("email").asText());
    }

    @Test
    void testLoginRejectsMismatchedRoleChoice() throws Exception {
        String tokenPayload = "{\"access_token\":\"valid-token\",\"token_type\":\"bearer\"}";
        startMockSupabaseServer(200, tokenPayload);
        authService.setSupabaseConfig(mockSupabaseUrl, "test-anon-key");

        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(userId);
        profile.setEmail("student@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        when(profileRepository.findByEmailIgnoreCase("student@srmist.edu.in")).thenReturn(Optional.of(profile));

        LoginRequest req = new LoginRequest("student@srmist.edu.in", "correct-password", "Maintenance Staff");
        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    private void startMockSupabaseServer(int statusCode, String responseJson) throws Exception {
        mockSupabaseServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockSupabaseServer.createContext("/auth/v1/token", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                byte[] inBytes = is.readAllBytes();
                lastRequestBody.set(new String(inBytes, StandardCharsets.UTF_8));
            }
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockSupabaseServer.start();
        mockSupabaseUrl = "http://127.0.0.1:" + mockSupabaseServer.getAddress().getPort();
    }
}
