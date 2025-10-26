package com.btl.serverapp.service;

import com.btl.serverapp.entity.ViolationLog;
import com.btl.serverapp.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

@Service
public class ProcessingService {

    @Autowired
    private LogRepository logRepository;

    // Runs the Python process (ProcessBuilder) and stores a ViolationLog
    public ViolationLog runProcessing(File videoFile, String lineConfig) {
        ViolationLog log = new ViolationLog();
        log.setVideoPath(videoFile.getAbsolutePath());
        log.setLineConfig(lineConfig);
        log.setTimestamp(LocalDateTime.now());

        try {
            // Path to python script - configure in application.properties as needed
            String script = System.getProperty("model.scripts.path", "../Model_Scripts/process.py");
            ProcessBuilder pb = new ProcessBuilder("python", script, "--video", videoFile.getAbsolutePath(), "--line", lineConfig == null ? "" : lineConfig);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            int exit = p.waitFor();
            log.setResult("exit=" + exit + " output=" + out.toString());
        } catch (Exception e) {
            log.setResult("error: " + e.getMessage());
        }

        // Persist log
        return logRepository.save(log);
    }
}
