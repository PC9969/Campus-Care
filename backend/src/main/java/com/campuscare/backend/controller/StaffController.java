package com.campuscare.backend.controller;

import com.campuscare.backend.dto.StaffResponse;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final ProfileService profileService;

    public StaffController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> getStaff(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<StaffResponse> staff = profileService.getAllStaff();
        return ResponseEntity.ok(staff);
    }
}
