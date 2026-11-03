package com.campuscare.backend.mapper;

import com.campuscare.backend.entity.IssueStatus;

public final class StatusMapper {

    private StatusMapper() {}

    public static String toFrontendString(IssueStatus status) {
        if (status == null) return "Open";
        return switch (status) {
            case OPEN -> "Open";
            case IN_PROGRESS -> "In Progress";
            case RESOLVED -> "Resolved";
            case CLOSED -> "Closed";
        };
    }

    public static IssueStatus fromFrontendString(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return IssueStatus.OPEN;
        }
        String clean = statusStr.trim().toLowerCase();
        return switch (clean) {
            case "open", "reported" -> IssueStatus.OPEN;
            case "in progress", "in_progress", "inprogress" -> IssueStatus.IN_PROGRESS;
            case "resolved", "verified" -> IssueStatus.RESOLVED;
            case "closed" -> IssueStatus.CLOSED;
            default -> {
                try {
                    yield IssueStatus.valueOf(statusStr.trim().toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException e) {
                    yield IssueStatus.OPEN;
                }
            }
        };
    }
}
