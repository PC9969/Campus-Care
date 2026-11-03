package com.campuscare.backend.controller;

import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserProfileResponse profile = profileService.getCurrentUserProfile(principal.getId());
        return ResponseEntity.ok(profile);
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only administrators can view all user profiles");
        }
        List<UserProfileResponse> users = profileService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal.getRole() != UserRole.ADMIN && !principal.getId().equals(id)) {
            throw new UnauthorizedException("You are not authorized to view another user's profile");
        }
        UserProfileResponse profile = profileService.getCurrentUserProfile(id);
        return ResponseEntity.ok(profile);
    }
}
