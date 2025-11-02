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

    /**
     * Xử lý video phát hiện vi phạm
     * @param videoFile Video upload từ user
     * @param lineData JSON chứa tọa độ vạch dừng
     * @return JSON string chứa danh sách vi phạm
     */
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

        // --- BƯỚC 2: GỌI SCRIPT PYTHON QUA PROCESSBUILDER ---
        try {
            System.out.println("Bắt đầu xử lý video với Python script...");

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
            
            // KHÔNG merge stderr vào stdout - phải đọc riêng
            final Process process = pb.start();

            // Đọc stderr riêng (log info từ Python) - LƯU LẠI ĐỂ XEM LỖI
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

            // Đọc stdout (CHỈ JSON kết quả)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Chờ tiến trình kết thúc (timeout 10 phút)
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                process.destroy();
                throw new RuntimeException("Tiến trình Python mất quá 10 phút, đã hủy.");
            }

            int exitCode = process.exitValue();
            String jsonLogResult = output.toString().trim();

            if (exitCode != 0) {
                // Đợi thread stderr đọc xong
                errorReaderThread.join(1000);
                String errorMsg = errorOutput.toString();
                throw new RuntimeException("Lỗi script Python (exit code " + exitCode + "):\nSTDOUT: " + jsonLogResult + "\nSTDERR: " + errorMsg);
            }

            // --- BƯỚC 3: TRẢ KẾT QUẢ JSON ---
            // Không tự động lưu DB - để user chọn lưu trên frontend
            
            // Đảm bảo process kết thúc
            if (process.isAlive()) {
                process.destroy();
            }
            
            return jsonLogResult; // Trả chuỗi JSON về cho Controller

        } finally {
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
     * Lưu một vi phạm vào database (được gọi từ Controller khi user chọn lưu)
     * @param violation ViolationLog object từ frontend
     * @return true nếu lưu thành công
     */
    public Boolean saveViolation(ViolationLog violation) {
        return violationLogDAO.save(violation);
    }
}
