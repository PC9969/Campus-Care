package com.campuscare.backend.service;

import com.campuscare.backend.dto.LoginRequest;
import com.campuscare.backend.dto.LoginResponse;
import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.exception.ResourceNotFoundException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:}")
    private String supabaseAnonKey;

    public AuthService(
            ProfileRepository profileRepository,
            ProfileService profileService
    ) {
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void setSupabaseConfig(String supabaseUrl, String supabaseAnonKey) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();

        if (email.isBlank() || password.isEmpty()) {
            throw new BadRequestException("Email and password cannot be blank");
        }

        // 1. Verify credentials with Supabase Auth. Fails securely if unconfigured or unreachable.
        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseAnonKey == null || supabaseAnonKey.isBlank()) {
            throw new UnauthorizedException("Authentication service is unavailable: Supabase Auth is not configured");
        }

        String token;
        try {
            String authUrl = supabaseUrl.replaceAll("/+$", "") + "/auth/v1/token?grant_type=password";
            String body = objectMapper.writeValueAsString(new SupabaseAuthBody(email, password));

            HttpRequest authReq = HttpRequest.newBuilder()
                    .uri(URI.create(authUrl))
                    .header("apikey", supabaseAnonKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(authReq, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 400 || response.statusCode() == 401) {
                throw new UnauthorizedException("Invalid credentials. Please verify your email and password.");
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UnauthorizedException("Authentication failed: Supabase Auth error (status " + response.statusCode() + ")");
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("access_token") && !root.get("access_token").asText().isBlank()) {
                token = root.get("access_token").asText();
            } else {
                throw new UnauthorizedException("Authentication failed: Supabase did not return an access token");
            }
        } catch (UnauthorizedException ue) {
            throw ue;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new UnauthorizedException("Authentication service unreachable. Please try again later.");
        } catch (Exception e) {
            throw new UnauthorizedException("Authentication error: " + e.getMessage());
        }

        // 2. Fetch the corresponding profile from profiles table
        Profile profile = profileRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No campus profile registered for " + email));

        // 3. Enforce role choice category check if supplied by frontend
        String roleChoice = request.getRoleChoice();
        if (roleChoice != null && !roleChoice.isBlank()) {
            if ("Student / Professor".equalsIgnoreCase(roleChoice.trim())) {
                if (profile.getRole() != UserRole.STUDENT && profile.getRole() != UserRole.PROFESSOR) {
                    throw new BadRequestException("This account is not registered as a Student or Professor");
                }
            } else if ("Maintenance Staff".equalsIgnoreCase(roleChoice.trim())) {
                if (profile.getRole() != UserRole.MAINTENANCE) {
                    throw new BadRequestException("This account is not registered as Maintenance Staff");
                }
            }
        }

        UserProfileResponse userResponse = profileService.buildUserProfileResponse(profile);
        return new LoginResponse(token, userResponse);
    }

    public UserProfileResponse getCurrentUser(UUID userId) {
        return profileService.getCurrentUserProfile(userId);
    }

    private record SupabaseAuthBody(String email, String password) {}
}
