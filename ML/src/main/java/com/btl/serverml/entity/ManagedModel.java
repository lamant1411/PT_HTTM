package com.btl.serverml.entity;

import javax.persistence.*;

@Entity
public class ManagedModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Tên model, vd: "YOLOv8s - Phát hiện vật thể"
    private String description;
    
    // Đường dẫn tuyệt đối đến file .pt
    // Vd: "D:/PT_HTTM/Model_Scripts/models/yolov8_detect_xe.pt"
    @Column(nullable = false)
    private String filePath; 

    // Quan hệ với Bài toán con
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    // --- Logic "LOCK" ---
    // Chỉ có 1 model được lock=true cho mỗi Problem
    @Column(nullable = false)
    private boolean isLocked = false; 

    // Getters và Setters
    // (Bạn tự tạo các getter/setter cho các biến trên)
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}