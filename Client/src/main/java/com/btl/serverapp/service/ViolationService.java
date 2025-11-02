package com.btl.serverapp.service;

import com.btl.serverapp.dao.ViolationLogDAO;
import com.btl.serverapp.entity.ViolationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ViolationService {

    @Autowired
    private ViolationLogDAO violationLogDAO;

    // --- READ CONFIGURATION FROM PROPERTIES ---
    @Value("${model.path.object-detect}")
    private String objectModelPath;
    @Value("${model.path.plate-detect}")
    private String plateModelPath;
    @Value("${model.path.ocr}")
    private String ocrModelPath;

    @Value("${python.executable}")
    private String pythonExecutable;
    @Value("${python.script.path}")
    private String pythonScriptPath;

    /**
     * Process video to detect violations
     * @param videoFile Video file uploaded from user
     * @param lineData JSON containing stop line coordinates
     * @return JSON string containing list of violations
     */
    public String processVideo(MultipartFile videoFile, String lineData) throws Exception {

        // --- STEP 1: SAVE TEMP FILES ---
        Path tempDir = Paths.get("temp");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String uniqueID = UUID.randomUUID().toString();
        Path videoTempPath = tempDir.resolve(uniqueID + "_" + videoFile.getOriginalFilename());
        Path configTempPath = tempDir.resolve(uniqueID + "_config.json");

        // Đảm bảo đóng stream sau khi copy
        try (var inputStream = videoFile.getInputStream()) {
            Files.copy(inputStream, videoTempPath);
        }
        Files.write(configTempPath, lineData.getBytes());

        // --- STEP 2: CALL PYTHON SCRIPT VIA PROCESSBUILDER ---
        try {
            System.out.println("Starting video processing with Python script...");

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    pythonScriptPath,
                    "--video", videoTempPath.toAbsolutePath().toString(),
                    "--config", configTempPath.toAbsolutePath().toString(),
                    // Pass 3 model paths read from properties
                    "--object_model", objectModelPath,
                    "--plate_model", plateModelPath,
                    "--ocr_model", ocrModelPath
            );
            
            // Do NOT merge stderr into stdout - read separately
            final Process process = pb.start();

            // Read stderr separately (Python logs) - SAVE FOR ERROR CHECKING
            StringBuilder errorOutput = new StringBuilder();
            Thread errorReaderThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.out.println("[Python Log]: " + line);
                        errorOutput.append(line).append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            errorReaderThread.start();

            // Read stdout (ONLY JSON result)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for process to finish (timeout 10 minutes)
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                process.destroy();
                throw new RuntimeException("Python process timeout after 10 minutes, terminated.");
            }

            int exitCode = process.exitValue();
            String jsonLogResult = output.toString().trim();

            if (exitCode != 0) {
                // Wait for stderr thread to finish reading
                errorReaderThread.join(1000);
                String errorMsg = errorOutput.toString();
                throw new RuntimeException("Python script error (exit code " + exitCode + "):\nSTDOUT: " + jsonLogResult + "\nSTDERR: " + errorMsg);
            }

            // --- STEP 3: RETURN JSON RESULT ---
            // Do not auto-save to DB - let user confirm on frontend
            
            // Ensure process is terminated
            if (process.isAlive()) {
                process.destroy();
            }
            
            return jsonLogResult; // Return JSON string to Controller

        } finally {
            // --- STEP 4: CLEANUP TEMP FILES ---
            // Add small delay to ensure Python has released files (important on Windows)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Try to delete files, only log warning if fails
            try {
                Files.deleteIfExists(videoTempPath);
                Files.deleteIfExists(configTempPath);
                System.out.println("Temp files cleaned up.");
            } catch (Exception e) {
                System.err.println("Warning: Cannot delete temp files immediately: " + e.getMessage());
                // Files will be deleted by OS or next cleanup
            }
        }
    }
    
    /**
     * Save violation AND base64 image to database + file system
     * @param licensePlate License plate number
     * @param imageBase64 Base64 string of image (format: "data:image/jpeg;base64,...")
     * @param logData Map containing all violation information
     * @return Saved ViolationLog or null if failed
     */
    public ViolationLog saveViolationWithImage(String licensePlate, String imageBase64, java.util.Map<String, Object> logData) {
        try {
            System.out.println("[DEBUG] Received base64 string, length: " + (imageBase64 != null ? imageBase64.length() : "null"));
            
            if (imageBase64 == null || imageBase64.isEmpty()) {
                System.err.println("[ERROR] Base64 string is empty!");
                return null;
            }
            
            // 1. Decode base64 → byte array
            String base64Data = imageBase64;
            
            // Remove "data:image/jpeg;base64," prefix if exists
            if (imageBase64.startsWith("data:")) {
                int commaIndex = imageBase64.indexOf(",");
                if (commaIndex > 0) {
                    base64Data = imageBase64.substring(commaIndex + 1);
                    System.out.println("[DEBUG] Removed prefix, remaining length: " + base64Data.length());
                }
            }
            
            // Clean whitespace and newlines
            base64Data = base64Data.replaceAll("\\s+", "");
            
            System.out.println("[DEBUG] Start decoding base64...");
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            System.out.println("[DEBUG] Decode successful, byte array size: " + imageBytes.length);
            
            // 2. Create unique filename
            String fileName = "violation_" + java.util.UUID.randomUUID().toString() + ".jpg";
            
            // 3. Save file to static/violation_images/
            Path imagePath = Paths.get("src/main/resources/static/violation_images/" + fileName);
            Files.createDirectories(imagePath.getParent());
            Files.write(imagePath, imageBytes);
            
            System.out.println("[SUCCESS] Image saved: " + imagePath.toAbsolutePath());
            
            // 4. Create ViolationLog entity
            ViolationLog log = new ViolationLog();
            log.setPlateNum(licensePlate != null ? licensePlate : "Unknown");
            log.setEvidenceUrl("/violation_images/" + fileName);
            log.setTimestamp(java.time.LocalDateTime.now());
            
            // Save full JSON to logDetails
            log.setLogDetails(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(logData));
            
            // 5. Save to database
            Boolean saved = violationLogDAO.save(log);
            
            return saved ? log : null;
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to save image/database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Save a violation to database (called from Controller when user confirms)
     * @param violation ViolationLog object from frontend
     * @return true if save successful
     */
    public Boolean saveViolation(ViolationLog violation) {
        return violationLogDAO.save(violation);
    }
}
