package com.campuscare.backend.dto;

public class WeeklyTrendDto {

    private String day;
    private long reported;
    private long resolved;

    public WeeklyTrendDto() {}

    public WeeklyTrendDto(String day, long reported, long resolved) {
        this.day = day;
        this.reported = reported;
        this.resolved = resolved;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getReported() {
        return reported;
    }

    public void setReported(long reported) {
        this.reported = reported;
    }

    public long getResolved() {
        return resolved;
    }

    public void setResolved(long resolved) {
        this.resolved = resolved;
    }
}
