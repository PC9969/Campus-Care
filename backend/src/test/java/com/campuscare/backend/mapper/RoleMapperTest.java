package com.campuscare.backend.mapper;

import com.campuscare.backend.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleMapperTest {

    @Test
    void testToFrontendString() {
        assertEquals("Student", RoleMapper.toFrontendString(UserRole.STUDENT));
        assertEquals("Professor", RoleMapper.toFrontendString(UserRole.PROFESSOR));
        assertEquals("Maintenance Staff", RoleMapper.toFrontendString(UserRole.MAINTENANCE));
        assertEquals("Admin", RoleMapper.toFrontendString(UserRole.ADMIN));
    }

    @Test
    void testFromFrontendString() {
        assertEquals(UserRole.STUDENT, RoleMapper.fromFrontendString("Student"));
        assertEquals(UserRole.STUDENT, RoleMapper.fromFrontendString("STUDENT"));
        assertEquals(UserRole.PROFESSOR, RoleMapper.fromFrontendString("Professor"));
        assertEquals(UserRole.MAINTENANCE, RoleMapper.fromFrontendString("Maintenance Staff"));
        assertEquals(UserRole.MAINTENANCE, RoleMapper.fromFrontendString("maintenance"));
        assertEquals(UserRole.ADMIN, RoleMapper.fromFrontendString("Admin"));
        assertEquals(UserRole.ADMIN, RoleMapper.fromFrontendString("Administrator"));
    }
}
