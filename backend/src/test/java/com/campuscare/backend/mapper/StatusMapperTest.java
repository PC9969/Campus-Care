package com.campuscare.backend.mapper;

import com.campuscare.backend.entity.IssueStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusMapperTest {

    @Test
    void testToFrontendString() {
        assertEquals("Open", StatusMapper.toFrontendString(IssueStatus.OPEN));
        assertEquals("In Progress", StatusMapper.toFrontendString(IssueStatus.IN_PROGRESS));
        assertEquals("Resolved", StatusMapper.toFrontendString(IssueStatus.RESOLVED));
        assertEquals("Closed", StatusMapper.toFrontendString(IssueStatus.CLOSED));
    }

    @Test
    void testFromFrontendString() {
        assertEquals(IssueStatus.OPEN, StatusMapper.fromFrontendString("Open"));
        assertEquals(IssueStatus.OPEN, StatusMapper.fromFrontendString("Reported"));
        assertEquals(IssueStatus.IN_PROGRESS, StatusMapper.fromFrontendString("In Progress"));
        assertEquals(IssueStatus.IN_PROGRESS, StatusMapper.fromFrontendString("IN_PROGRESS"));
        assertEquals(IssueStatus.RESOLVED, StatusMapper.fromFrontendString("Resolved"));
        assertEquals(IssueStatus.RESOLVED, StatusMapper.fromFrontendString("Verified"));
        assertEquals(IssueStatus.CLOSED, StatusMapper.fromFrontendString("Closed"));
    }
}
