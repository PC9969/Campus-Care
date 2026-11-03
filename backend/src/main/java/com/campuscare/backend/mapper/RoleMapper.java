package com.campuscare.backend.mapper;

import com.campuscare.backend.entity.UserRole;

public final class RoleMapper {

    private RoleMapper() {}

    public static String toFrontendString(UserRole role) {
        if (role == null) return "Student";
        return switch (role) {
            case STUDENT -> "Student";
            case PROFESSOR -> "Professor";
            case MAINTENANCE -> "Maintenance Staff";
            case ADMIN -> "Admin";
        };
    }

    public static UserRole fromFrontendString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return UserRole.STUDENT;
        }
        String clean = roleStr.trim().toLowerCase();
        return switch (clean) {
            case "student" -> UserRole.STUDENT;
            case "professor" -> UserRole.PROFESSOR;
            case "maintenance staff", "maintenance", "staff" -> UserRole.MAINTENANCE;
            case "admin", "administrator" -> UserRole.ADMIN;
            default -> {
                try {
                    yield UserRole.valueOf(roleStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield UserRole.STUDENT;
                }
            }
        };
    }
}
