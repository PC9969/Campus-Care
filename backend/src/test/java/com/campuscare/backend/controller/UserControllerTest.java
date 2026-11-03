package com.campuscare.backend.controller;

import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.security.CustomUserPrincipal;
import com.campuscare.backend.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private ProfileService profileService;

    private UserController userController;

    private UUID studentId;
    private UUID otherUserId;
    private UUID adminId;

    private CustomUserPrincipal studentPrincipal;
    private CustomUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        userController = new UserController(profileService);

        studentId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        Profile studentProfile = new Profile();
        studentProfile.setId(studentId);
        studentProfile.setEmail("student@srmist.edu.in");
        studentProfile.setRole(UserRole.STUDENT);
        studentPrincipal = new CustomUserPrincipal(studentProfile);

        Profile adminProfile = new Profile();
        adminProfile.setId(adminId);
        adminProfile.setEmail("admin@srmist.edu.in");
        adminProfile.setRole(UserRole.ADMIN);
        adminPrincipal = new CustomUserPrincipal(adminProfile);
    }

    @Test
    void testNonAdminCannotGetAllUsers() {
        assertThrows(UnauthorizedException.class, () ->
                userController.getAllUsers(studentPrincipal)
        );
        verifyNoInteractions(profileService);
    }

    @Test
    void testAdminCanGetAllUsers() {
        when(profileService.getAllUsers()).thenReturn(List.of(new UserProfileResponse()));

        ResponseEntity<List<UserProfileResponse>> response = userController.getAllUsers(adminPrincipal);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testUserCanGetOwnProfileById() {
        UserProfileResponse myProfile = new UserProfileResponse();
        myProfile.setId(studentId);
        when(profileService.getCurrentUserProfile(studentId)).thenReturn(myProfile);

        ResponseEntity<UserProfileResponse> response = userController.getUserById(studentId, studentPrincipal);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(studentId, response.getBody().getId());
    }

    @Test
    void testNonAdminCannotGetOtherUserProfileById() {
        assertThrows(UnauthorizedException.class, () ->
                userController.getUserById(otherUserId, studentPrincipal)
        );
        verify(profileService, never()).getCurrentUserProfile(otherUserId);
    }

    @Test
    void testAdminCanGetOtherUserProfileById() {
        UserProfileResponse otherProfile = new UserProfileResponse();
        otherProfile.setId(otherUserId);
        when(profileService.getCurrentUserProfile(otherUserId)).thenReturn(otherProfile);

        ResponseEntity<UserProfileResponse> response = userController.getUserById(otherUserId, adminPrincipal);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(otherUserId, response.getBody().getId());
    }
}
