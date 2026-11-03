package com.campuscare.backend.dto;

public class CategoryResponse {

    private Long id;
    private String name;
    private String category; // alias for frontend
    private String description;
    private long count;

    public CategoryResponse() {}

    public CategoryResponse(Long id, String name, String description, long count) {
        this.id = id;
        this.name = name;
        this.category = name;
        this.description = description;
        this.count = count;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.category = name;
    }

    public String getCategory() {
        return category != null ? category : name;
    }

    public void setCategory(String category) {
        this.category = category;
        if (this.name == null) {
            this.name = category;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
