package com.campuscare.backend.dto;

public class EvidenceRequest {

    private String name;
    private String type;
    private String size;
    private String url;

    public EvidenceRequest() {}

    public EvidenceRequest(String name, String type, String size, String url) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
