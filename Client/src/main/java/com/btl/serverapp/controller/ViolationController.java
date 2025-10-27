package com.btl.serverapp.controller;

import com.btl.serverapp.dao.ViolationLogDAO;
import com.btl.serverapp.entity.ViolationLog;
import com.btl.serverapp.service.ViolationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;


@RestController
@RequestMapping("/api/v1")
public class ViolationController {

    @Autowired
    private ViolationService violationService;

    @Autowired
    private ViolationLogDAO violationLogDAO;

    @PostMapping("/process")
    public ResponseEntity<String> handleVideo(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("lineData") String lineData) {
        
        try {
            // Gọi service để xử lý và chờ kết quả
            String resultLog = violationService.processVideo(videoFile, lineData);
            
            // Trả log JSON về cho frontend
            return ResponseEntity.ok(resultLog);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi server: " + e.getMessage());
        }
    }

    @PostMapping("/violations/save")
    public ResponseEntity<?> saveViolation(@RequestBody Map<String, Object> logData) {
        try {
            // Map JSON từ frontend sang ViolationLog entity
            ViolationLog log = new ViolationLog();
            
            // Lấy license_plate từ JSON
            String licensePlate = (String) logData.get("license_plate");
            log.setPlateNum(licensePlate != null ? licensePlate : "Không xác định");
            
            // Lấy evidence_url từ JSON
            String evidenceUrl = (String) logData.get("evidence_url");
            log.setEvidenceUrl(evidenceUrl);
            
            // Timestamp sẽ được tự động set bởi @PrePersist
            log.setTimestamp(LocalDateTime.now());
            
            // Lưu toàn bộ JSON vào logDetails
            log.setLogDetails(new ObjectMapper().writeValueAsString(logData));
            
            Boolean saved = violationLogDAO.save(log);
            if (saved) {
                return ResponseEntity.ok(log);
            } else {
                return ResponseEntity.status(500).body("Không thể lưu vi phạm");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi khi lưu: " + e.getMessage());
        }
    }
}