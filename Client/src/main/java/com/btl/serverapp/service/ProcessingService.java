package com.btl.serverapp.service;

import com.btl.serverapp.entity.ViolationLog;
import com.btl.serverapp.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import @Value
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
// XÓA import WebClient và Mono

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessingService {

    @Autowired
    private LogRepository logRepository;

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
    
    public String processVideo(MultipartFile videoFile, String lineDataJson) throws Exception {

        // --- BƯỚC 1: LƯU FILE TẠM ---
        Path tempDir = Paths.get("temp");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String uniqueID = UUID.randomUUID().toString();
        Path videoTempPath = tempDir.resolve(uniqueID + "_" + videoFile.getOriginalFilename());
        Path configTempPath = tempDir.resolve(uniqueID + "_config.json");

        Files.copy(videoFile.getInputStream(), videoTempPath);
        Files.write(configTempPath, lineDataJson.getBytes());

        // --- BƯỚC 2: GỌI SCRIPT PYTHON (KHÔNG HỎI ML) ---
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
            Process process = pb.start();

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

            // --- BƯỚC 3: LƯU LOG VÀ TRẢ KẾT QUẢ ---
            ViolationLog logEntry = new ViolationLog();
            logEntry.setLogDetails(jsonLogResult);
            logRepository.save(logEntry);

            return jsonLogResult; // Trả chuỗi JSON về cho Controller

        } finally {
            // --- BƯỚC 4: DỌN DẸP FILE TẠM ---
            Files.deleteIfExists(videoTempPath);
            Files.deleteIfExists(configTempPath);
            System.out.println("Đã dọn dẹp file tạm.");
        }
    }
    
    // XÓA hàm private "getLockedModelPath"
}