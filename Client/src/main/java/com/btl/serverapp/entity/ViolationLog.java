package com.btl.serverapp.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ViolationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime timestamp;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String logDetails; // Lưu chuỗi JSON log từ Python

    // Getters, Setters...

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
    
    // (Tạo getter/setter cho id và logDetails)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogDetails() { return logDetails; }
    public void setLogDetails(String logDetails) { this.logDetails = logDetails; }
}