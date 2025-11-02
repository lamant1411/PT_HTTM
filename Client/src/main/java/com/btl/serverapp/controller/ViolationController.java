package com.btl.serverapp.controller;

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

    /**
     * Endpoint to process video (Steps 1-8: Upload → Python → Parse → Return)
     * @param videoFile Video file uploaded from user
     * @param lineData JSON containing stop line coordinates
     * @return JSON array containing list of violations
     */
    @PostMapping("/process")
    public ResponseEntity<String> handleVideo(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("lineData") String lineData) {
        
        try {
            // Call service to process and wait for result
            String resultLog = violationService.processVideo(videoFile, lineData);
            
            // Return JSON log to frontend
            return ResponseEntity.ok(resultLog);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    /**
     * Endpoint to save violation to database (Step 10 - User confirms save)
     * @param logData JSON object containing violation info from frontend
     * @return Saved ViolationLog or error message
     */
    @PostMapping("/violations/save")
    public ResponseEntity<?> saveViolation(@RequestBody Map<String, Object> logData) {
        try {
            // Get license_plate from JSON
            String licensePlate = (String) logData.get("license_plate");
            
            // Get base64 image from JSON
            String imageBase64 = (String) logData.get("evidence_image_base64");
            
            // Call service to save (service will decode base64 → save file → return path)
            ViolationLog savedLog = violationService.saveViolationWithImage(licensePlate, imageBase64, logData);
            
            if (savedLog != null) {
                return ResponseEntity.ok(savedLog);
            } else {
                return ResponseEntity.status(500).body("Cannot save violation");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving: " + e.getMessage());
        }
    }
}