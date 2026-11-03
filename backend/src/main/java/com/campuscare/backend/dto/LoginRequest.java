package com.campuscare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid college email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String roleChoice;

    public LoginRequest() {}

    public LoginRequest(String email, String password, String roleChoice) {
        this.email = email;
        this.password = password;
        this.roleChoice = roleChoice;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoleChoice() {
        return roleChoice;
    }

    public void setRoleChoice(String roleChoice) {
        this.roleChoice = roleChoice;
    }
}
