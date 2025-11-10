package com.btl.serverapp.controller;

import com.btl.serverapp.service.ViolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;


@RestController
@RequestMapping("/api/v1")
public class ViolationController {

    @Autowired
    private ViolationService violationService;

    /**
     * Xử lý video gửi từ frontend để phát hiện vi phạm
     * @param videoFile 
     * @param lineData 
     * @return mảng json log về các vi phạm phát hiện được
     */
    @PostMapping("/process")
    public ResponseEntity<String> handleVideo(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("lineData") String lineData) {
        
        try {
            // gọi service xử lý video
            String resultLog = violationService.processVideo(videoFile, lineData);

            // Trả về JSON log cho frontend
            return ResponseEntity.ok(resultLog);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    /**
     * lưu vi phạm từ frontend gửi lên
     * @param logData dữ liệu vi phạm từ frontend
     * @return ResponseEntity với thông báo thành công hoặc lỗi
     */
    @PostMapping("/violations/save")
    public ResponseEntity<?> saveViolationLog(@RequestBody Map<String, Object> logData) {
        try {
            String licensePlate = (String) logData.get("license_plate");
            // Ảnh ở frontend được mã hóa base64 để tránh việc lưu file tạm thời
            String imageBase64 = (String) logData.get("evidence_image_base64");
            String plateImageBase64 = (String) logData.get("plate_image_base64");
            
            Boolean success = violationService.saveViolationLog(licensePlate, imageBase64, plateImageBase64);
            
            if (success) {
                return ResponseEntity.ok().body("{\"message\": \"Lưu vi phạm thành công\", \"success\": true}");
            } else {
                return ResponseEntity.status(500).body("{\"message\": \"Không thể lưu vi phạm\", \"success\": false}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"message\": \"Lỗi khi lưu: " + e.getMessage() + "\", \"success\": false}");
        }
    }
}