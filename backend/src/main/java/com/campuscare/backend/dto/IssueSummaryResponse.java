package com.campuscare.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class IssueSummaryResponse {

    private long open;
    private long inProgress;
    private long resolved;
    private long closed;
    private long total;
    private long newThisWeek;
    private String avgResolution;
    private List<WeeklyTrendDto> weekly = new ArrayList<>();
    private List<CategoryCountDto> categories = new ArrayList<>();
    private List<PriorityCountDto> priorities = new ArrayList<>();

    public IssueSummaryResponse() {}

    public long getOpen() {
        return open;
    }

    public void setOpen(long open) {
        this.open = open;
    }

    public long getInProgress() {
        return inProgress;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    public long getResolved() {
        return resolved;
    }

    public void setResolved(long resolved) {
        this.resolved = resolved;
    }

    public long getClosed() {
        return closed;
    }

    public void setClosed(long closed) {
        this.closed = closed;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getNewThisWeek() {
        return newThisWeek;
    }

    public void setNewThisWeek(long newThisWeek) {
        this.newThisWeek = newThisWeek;
    }

    public String getAvgResolution() {
        return avgResolution;
    }

    public void setAvgResolution(String avgResolution) {
        this.avgResolution = avgResolution;
    }

    public List<WeeklyTrendDto> getWeekly() {
        return weekly;
    }

    public void setWeekly(List<WeeklyTrendDto> weekly) {
        this.weekly = weekly;
    }

    public List<CategoryCountDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryCountDto> categories) {
        this.categories = categories;
    }

    public List<PriorityCountDto> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<PriorityCountDto> priorities) {
        this.priorities = priorities;
    }
}
