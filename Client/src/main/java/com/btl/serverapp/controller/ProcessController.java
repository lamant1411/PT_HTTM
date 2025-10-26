package com.btl.serverapp.controller;

import com.btl.serverapp.service.ProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ProcessController {

    @Autowired
    private ProcessingService processingService;

    @PostMapping("/process")
    public ResponseEntity<String> handleProcessing(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("lineData") String lineDataJson) {
        
        try {
            // Gọi service để xử lý và chờ kết quả
            String resultLog = processingService.processVideo(videoFile, lineDataJson);
            
            // Trả log JSON về cho frontend
            return ResponseEntity.ok(resultLog);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi server: " + e.getMessage());
        }
    }
}