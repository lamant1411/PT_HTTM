package com.btl.serverapp.entity;
import java.time.LocalDateTime;

public class ViolationLog {

    private int id;
    private String plateNum;
    private LocalDateTime timestamp;
    private String evidenceUrl;
    private String plateImageUrl;

    public ViolationLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getPlateNum() {
        return plateNum;
    }
    
    public void setPlateNum(String plateNum) {
        this.plateNum = plateNum;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEvidenceUrl() {
        return evidenceUrl;
    }
    
    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getPlateImageUrl() {
        return plateImageUrl;
    }

    public void setPlateImageUrl(String plateImageUrl) {
        this.plateImageUrl = plateImageUrl;
    }
}