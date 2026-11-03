package com.campuscare.backend.controller;

import com.campuscare.backend.dto.LoginRequest;
import com.campuscare.backend.dto.LoginResponse;
import com.campuscare.backend.dto.UserProfileResponse;
import com.campuscare.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest("rahul.s@srmist.edu.in", "password123", "Student / Professor");

        UserProfileResponse user = new UserProfileResponse();
        user.setId(UUID.randomUUID());
        user.setName("Rahul Sharma");
        user.setEmail("rahul.s@srmist.edu.in");
        user.setRole("Student");

        LoginResponse loginResp = new LoginResponse("mock-jwt-token-xyz", user);
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResp);

        ResponseEntity<LoginResponse> response = authController.login(req);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("mock-jwt-token-xyz", response.getBody().getToken());
        assertEquals("Rahul Sharma", response.getBody().getUser().getName());
        assertEquals("Student", response.getBody().getUser().getRole());
    }

    @Test
    void testLogoutSuccess() {
        ResponseEntity<Void> response = authController.logout();
        assertEquals(204, response.getStatusCode().value());
    }
}
