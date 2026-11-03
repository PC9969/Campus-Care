package com.campuscare.backend.mapper;

import com.campuscare.backend.entity.IssuePriority;

public final class PriorityMapper {

    private PriorityMapper() {}

    public static String toFrontendString(IssuePriority priority) {
        if (priority == null) return "Medium";
        return switch (priority) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
            case CRITICAL -> "Critical";
        };
    }

    public static IssuePriority fromFrontendString(String priorityStr) {
        if (priorityStr == null || priorityStr.trim().isEmpty()) {
            return IssuePriority.MEDIUM;
        }
        String clean = priorityStr.trim().toLowerCase();
        return switch (clean) {
            case "low" -> IssuePriority.LOW;
            case "medium" -> IssuePriority.MEDIUM;
            case "high" -> IssuePriority.HIGH;
            case "critical" -> IssuePriority.CRITICAL;
            default -> {
                try {
                    yield IssuePriority.valueOf(priorityStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield IssuePriority.MEDIUM;
                }
            }
        };
    }
}
