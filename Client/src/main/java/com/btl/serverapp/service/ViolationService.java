package com.btl.serverapp.service;

import com.btl.serverapp.dao.ViolationLogDAO;
import com.btl.serverapp.entity.ViolationLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // --- ĐỌC CẤU HÌNH TỪ PROPERTIES ---
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

    // XÓA hàm khởi tạo (constructor) của WebClient
    
    public String processVideo(MultipartFile videoFile, String lineData) throws Exception {

        // --- BƯỚC 1: LƯU FILE TẠM ---
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

        // --- BƯỚC 2: GỌI SCRIPT PYTHON (KHÔNG HỎI ML) ---
        Process process = null;
        try {
            System.out.println("Bắt đầu xử lý Python với model viết chết...");

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    pythonScriptPath,
                    "--video", videoTempPath.toAbsolutePath().toString(),
                    "--config", configTempPath.toAbsolutePath().toString(),
                    // Truyền 3 đường dẫn model đã đọc từ properties
                    "--object_model", objectModelPath,
                    "--plate_model", plateModelPath,
                    "--ocr_model", ocrModelPath
            );
            
            pb.redirectErrorStream(true);
            process = pb.start();

            // Đọc output từ Python
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python Log]: " + line);
                    output.append(line);
                }
            }

            // Chờ tiến trình kết thúc (timeout 10 phút)
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                process.destroy();
                throw new RuntimeException("Tiến trình Python mất quá 10 phút, đã hủy.");
            }

            int exitCode = process.exitValue();
            String jsonLogResult = output.toString();

            if (exitCode != 0) {
                throw new RuntimeException("Lỗi script Python (exit code " + exitCode + "): " + jsonLogResult);
            }

            // --- BƯỚC 3: PARSE JSON VÀ LƯU TỪNG VI PHẠM ---
            saveViolationLogs(jsonLogResult);

            return jsonLogResult; // Trả chuỗi JSON về cho Controller

        } finally {
            // Đảm bảo process được destroy
            if (process != null && process.isAlive()) {
                process.destroy();
            }
            
            // --- BƯỚC 4: DỌN DẸP FILE TẠM ---
            // Thêm delay nhỏ để đảm bảo Python đã release file (quan trọng trên Windows)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Thử xóa file, nếu không được thì chỉ log warning
            try {
                Files.deleteIfExists(videoTempPath);
                Files.deleteIfExists(configTempPath);
                System.out.println("Đã dọn dẹp file tạm.");
            } catch (Exception e) {
                System.err.println("Cảnh báo: Không thể xóa file tạm ngay lập tức: " + e.getMessage());
                // File sẽ bị xóa bởi OS hoặc lần cleanup tiếp theo
            }
        }
    }
    
    /**
     * Parse JSON từ Python và lưu từng vi phạm vào database
     * @param result Chuỗi JSON trả về từ Python script
     * @return true nếu parse và lưu thành công, false nếu không có dữ liệu
     */
    private Boolean saveViolationLogs(String result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(result);
            
            if (rootNode.isArray()) {
                int savedCount = 0;
                for (JsonNode violationNode : rootNode) {
                    ViolationLog logEntry = new ViolationLog();
                    
                    // Lấy thông tin từ JSON
                    if (violationNode.has("license_plate")) {
                        logEntry.setPlateNum(violationNode.get("license_plate").asText());
                    }
                    
                    if (violationNode.has("evidence_url")) {
                        logEntry.setEvidenceUrl(violationNode.get("evidence_url").asText());
                    }
                    
                    // Lưu chi tiết bổ sung (frame, etc.)
                    logEntry.setLogDetails(violationNode.toString());
                    
                    // Lưu vào database
                    if (violationLogDAO.save(logEntry)) {
                        savedCount++;
                    }
                }
                return savedCount > 0;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Không thể parse JSON: " + e.getMessage());
            throw new RuntimeException("Lỗi parse kết quả từ Python: " + e.getMessage());
        }
    }
}
