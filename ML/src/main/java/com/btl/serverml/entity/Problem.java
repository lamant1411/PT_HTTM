package com.btl.serverml.entity;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên bài toán con, vd: "Phát hiện vật thể", "Xác định biển số"
    @Column(unique = true, nullable = false)
    private String name;

    // Một Problem có nhiều Model
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ManagedModel> models;
    
    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<ManagedModel> getModels() { return models; }
    public void setModels(Set<ManagedModel> models) { this.models = models; }
}