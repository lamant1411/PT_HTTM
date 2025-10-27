package com.btl.serverml.controller;

import com.btl.serverml.dao.ManagedModelDAO;
import com.btl.serverml.dao.ProblemDAO;
import com.btl.serverml.entity.ManagedModel;
import com.btl.serverml.entity.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho ML Admin
 */
@RestController
@RequestMapping("/api")
public class MLApiController {

    @Autowired
    private ProblemDAO problemDAO;

    @Autowired
    private ManagedModelDAO modelDAO;

    // ========== PROBLEM APIs ==========

    /**
     * Lấy tất cả Problems
     */
    @GetMapping("/problems")
    public ResponseEntity<List<Problem>> getAllProblems() {
        return ResponseEntity.ok(problemDAO.findAll());
    }

    /**
     * Lấy Problem theo ID
     */
    @GetMapping("/problems/{id}")
    public ResponseEntity<Problem> getProblemById(@PathVariable Long id) {
        return problemDAO.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tạo Problem mới
     */
    @PostMapping("/problems")
    public ResponseEntity<Problem> createProblem(@RequestBody Problem problem) {
        if (problemDAO.existsByName(problem.getName())) {
            return ResponseEntity.badRequest().build();
        }
        Problem saved = problemDAO.save(problem);
        return ResponseEntity.ok(saved);
    }

    /**
     * Xóa Problem
     */
    @DeleteMapping("/problems/{id}")
    public ResponseEntity<String> deleteProblem(@PathVariable Long id) {
        if (problemDAO.deleteById(id)) {
            return ResponseEntity.ok("Đã xóa Problem #" + id);
        }
        return ResponseEntity.notFound().build();
    }

    // ========== MODEL APIs ==========

    /**
     * Lấy tất cả Models
     */
    @GetMapping("/models")
    public ResponseEntity<List<ManagedModel>> getAllModels() {
        return ResponseEntity.ok(modelDAO.findAll());
    }

    /**
     * Lấy Model theo ID
     */
    @GetMapping("/models/{id}")
    public ResponseEntity<ManagedModel> getModelById(@PathVariable Long id) {
        return modelDAO.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy tất cả Models của một Problem
     */
    @GetMapping("/problems/{problemId}/models")
    public ResponseEntity<List<ManagedModel>> getModelsByProblem(@PathVariable Long problemId) {
        return ResponseEntity.ok(modelDAO.findByProblemId(problemId));
    }

    /**
     * Lấy Model đang được lock của một Problem
     */
    @GetMapping("/problems/{problemId}/locked-model")
    public ResponseEntity<?> getLockedModel(@PathVariable Long problemId) {
        return modelDAO.findLockedModelByProblemId(problemId)
                .map(model -> ResponseEntity.ok(Map.of("filePath", model.getFilePath())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tạo Model mới
     */
    @PostMapping("/problems/{problemId}/models")
    public ResponseEntity<ManagedModel> createModel(
            @PathVariable Long problemId,
            @RequestBody ManagedModel model) {
        
        Problem problem = problemDAO.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại"));
        
        model.setProblem(problem);
        model.setLocked(false);
        ManagedModel saved = modelDAO.save(model);
        return ResponseEntity.ok(saved);
    }

    /**
     * Lock một Model
     */
    @PostMapping("/models/{modelId}/lock")
    public ResponseEntity<String> lockModel(@PathVariable Long modelId) {
        try {
            modelDAO.lockModel(modelId);
            return ResponseEntity.ok("Đã lock model #" + modelId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Unlock một Model
     */
    @PostMapping("/models/{modelId}/unlock")
    public ResponseEntity<String> unlockModel(@PathVariable Long modelId) {
        modelDAO.unlockModel(modelId);
        return ResponseEntity.ok("Đã unlock model #" + modelId);
    }

    /**
     * Xóa Model
     */
    @DeleteMapping("/models/{id}")
    public ResponseEntity<String> deleteModel(@PathVariable Long id) {
        if (modelDAO.deleteById(id)) {
            return ResponseEntity.ok("Đã xóa Model #" + id);
        }
        return ResponseEntity.notFound().build();
    }

    // ========== STATISTICS APIs ==========

    /**
     * Thống kê tổng quan
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Long totalProblems = problemDAO.count();
        Long totalModels = (long) modelDAO.findAll().size();
        
        return ResponseEntity.ok(Map.of(
            "totalProblems", totalProblems,
            "totalModels", totalModels
        ));
    }
}
