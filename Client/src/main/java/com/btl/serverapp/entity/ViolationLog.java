package com.btl.serverapp.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ViolationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Biển số xe vi phạm
    @Column(name = "plate_num")
    private String plateNum;
    
    // Thời gian vi phạm
    private LocalDateTime timestamp;
    
    // Đường dẫn đến ảnh bằng chứng
    @Column(name = "evidence_url")
    private String evidenceUrl;
    
    // Chi tiết bổ sung (có thể lưu thêm thông tin dạng JSON)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String logDetails;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
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
    
    public String getLogDetails() { 
        return logDetails; 
    }
    
    public void setLogDetails(String logDetails) { 
        this.logDetails = logDetails; 
    }
}