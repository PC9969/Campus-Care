package com.campuscare.backend.dto;

public class PriorityCountDto {

    private String priority;
    private long count;

    public PriorityCountDto() {}

    public PriorityCountDto(String priority, long count) {
        this.priority = priority;
        this.count = count;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
