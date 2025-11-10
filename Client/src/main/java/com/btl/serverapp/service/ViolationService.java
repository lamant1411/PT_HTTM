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
     * Process upload video để nhận diện
     * @param videoFile 
     * @param lineData 
     * @return JSON string kết quả log vi phạm từ Python script
     */
    public String processVideo(MultipartFile videoFile, String lineData) throws Exception {

        // --- lưu file tạm ---
        Path tempDir = Paths.get("temp");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String uniqueID = UUID.randomUUID().toString();
        Path videoTempPath = tempDir.resolve(uniqueID + "_" + videoFile.getOriginalFilename());
        Path configTempPath = tempDir.resolve(uniqueID + "_config.json");

        // Đóng stream sau khi copy
        try (var inputStream = videoFile.getInputStream()) {
            Files.copy(inputStream, videoTempPath);
        }
        Files.write(configTempPath, lineData.getBytes());

        // --- Gọi script Python lên nhận diện ---
        try {
            System.out.println("Starting video processing with Python script...");

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    pythonScriptPath,
                    "--video", videoTempPath.toAbsolutePath().toString(),
                    "--config", configTempPath.toAbsolutePath().toString(),
                    // 3 model tạm thời lấy cố định
                    "--object_model", objectModelPath,
                    "--plate_model", plateModelPath,
                    "--ocr_model", ocrModelPath
            );
        
            final Process process = pb.start();
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

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Chờ xhử lý (tối đa 5 phút)
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                process.destroy();
                throw new RuntimeException("Python process timeout after 10 minutes, terminated.");
            }

            int exitCode = process.exitValue();
            String jsonLogResult = output.toString().trim();

            if (exitCode != 0) {
                errorReaderThread.join(1000);
                String errorMsg = errorOutput.toString();
                throw new RuntimeException("Python script error (exit code " + exitCode + "):\nSTDOUT: " + jsonLogResult + "\nSTDERR: " + errorMsg);
            }

            if (process.isAlive()) {
                process.destroy();
            }
            
            return jsonLogResult;

        } finally {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Xóa file tạm
            try {
                Files.deleteIfExists(videoTempPath);
                Files.deleteIfExists(configTempPath);
                System.out.println("Temp files cleaned up.");
            } catch (Exception e) {
                System.err.println("Warning: Cannot delete temp files: " + e.getMessage());
            }
        }
    }
    
    /**
     * Lưu vi phạm vào database
     * @param licensePlate 
     * @param imageBase64 
     * @param plateImageBase64
     * @param logData
     * @return Boolean - true nếu lưu thành công, false nếu thất bại
     */
    public Boolean saveViolationLog(String licensePlate, String imageBase64, String plateImageBase64, java.util.Map<String, Object> logData) {
        try {
            System.out.println("[DEBUG] Received base64 string, length: " + (imageBase64 != null ? imageBase64.length() : "null"));
            
            if (imageBase64 == null || imageBase64.isEmpty()) {
                System.err.println("[ERROR] Base64 string is empty!");
                return false;
            }
            
            // Chuyển base64 thành file ảnh (evidence_image)
            String base64Data = imageBase64;
            
            // Xóa phần tiền tố nếu có
            if (imageBase64.startsWith("data:")) {
                int commaIndex = imageBase64.indexOf(",");
                if (commaIndex > 0) {
                    base64Data = imageBase64.substring(commaIndex + 1);
                    System.out.println("[DEBUG] Removed prefix, remaining length: " + base64Data.length());
                }
            }
            
            // Xóa khoảng trắng và dòng mới
            base64Data = base64Data.replaceAll("\\s+", "");
            
            System.out.println("[DEBUG] Start decoding base64...");
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            System.out.println("[DEBUG] Decode successful, byte array size: " + imageBytes.length);
            
            // Tạo tên file cho evidence image
            String fileName = "violation_" + java.util.UUID.randomUUID().toString() + ".jpg";

            // Lưu file ảnh vào thư mục violation_images
            Path imagePath = Paths.get("src/main/resources/static/violation_images/" + fileName);
            Files.createDirectories(imagePath.getParent());
            Files.write(imagePath, imageBytes);
            
            System.out.println("[SUCCESS] Evidence image saved: " + imagePath.toAbsolutePath());
            
            // Xử lý ảnh biển số nếu có
            String plateFileName = null;
            if (plateImageBase64 != null && !plateImageBase64.isEmpty()) {
                System.out.println("[DEBUG] Processing plate image, length: " + plateImageBase64.length());
                
                String plateBase64Data = plateImageBase64;
                
                // Xóa phần tiền tố nếu có
                if (plateImageBase64.startsWith("data:")) {
                    int commaIndex = plateImageBase64.indexOf(",");
                    if (commaIndex > 0) {
                        plateBase64Data = plateImageBase64.substring(commaIndex + 1);
                    }
                }
                
                // Xóa khoảng trắng và dòng mới
                plateBase64Data = plateBase64Data.replaceAll("\\s+", "");
                
                byte[] plateImageBytes = java.util.Base64.getDecoder().decode(plateBase64Data);
                System.out.println("[DEBUG] Plate image decoded, byte array size: " + plateImageBytes.length);
                
                // Tạo tên file cho plate image
                plateFileName = "plate_" + java.util.UUID.randomUUID().toString() + ".jpg";
                
                // Lưu file ảnh biển số
                Path plateImagePath = Paths.get("src/main/resources/static/violation_images/" + plateFileName);
                Files.write(plateImagePath, plateImageBytes);
                
                System.out.println("[SUCCESS] Plate image saved: " + plateImagePath.toAbsolutePath());
            } else {
                System.out.println("[INFO] No plate image provided");
            }
            
            // Tạo đối tượng ViolationLog
            ViolationLog log = new ViolationLog();
            log.setPlateNum(licensePlate != null ? licensePlate : "Unknown");
            log.setEvidenceUrl("/violation_images/" + fileName);
            log.setPlateImageUrl(plateFileName != null ? "/violation_images/" + plateFileName : null);
            // Không set timestamp vì DAO không lưu timestamp
            
            // 5. Lưu xuống csdl
            Boolean saved = violationLogDAO.save(log);
            
            return saved != null && saved;
            
        } catch (Exception e) {
            System.err.println("[ERROR] loi khi luu log vao csdl: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
