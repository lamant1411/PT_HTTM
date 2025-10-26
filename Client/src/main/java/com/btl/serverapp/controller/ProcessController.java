package com.btl.serverapp.controller;

import com.btl.serverapp.entity.ViolationLog;
import com.btl.serverapp.service.ProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @Autowired
    private ProcessingService processingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("video") MultipartFile video,
                                         @RequestParam(name = "lineConfig", required = false) String lineConfig) throws IOException {
        // Save to temp directory
        File tempDir = new File("temp");
        if (!tempDir.exists()) tempDir.mkdirs();
        File target = new File(tempDir, System.currentTimeMillis() + "_upload.mp4");
        video.transferTo(target);

        ViolationLog log = processingService.runProcessing(target, lineConfig == null ? "" : lineConfig);
        return ResponseEntity.ok(log);
    }
}
