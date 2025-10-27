package com.btl.serverml.controller;

import com.btl.serverml.dao.ManagedModelDAO;
import com.btl.serverml.dao.ProblemDAO;
import com.btl.serverml.entity.ManagedModel;
import com.btl.serverml.entity.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class ModelController {

    @Autowired
    private ProblemDAO problemDAO;

    @Autowired
    private ManagedModelDAO modelDAO;

    // --- PHẦN 1: API CHO PROJECT "CLIENT" CỦA BẠN ---
    // (Đây là API mà ProcessingService của bạn sẽ gọi)
    
    /**
     * API để lấy model đang được "lock" (sử dụng) cho một bài toán con.
     */
    @GetMapping("/api/problems/{id}/locked-model")
    @ResponseBody // Trả về JSON
    public ResponseEntity<?> getLockedModelForProblem(@PathVariable Long id) {
        Problem problem = problemDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Problem " + id));

        ManagedModel lockedModel = modelDAO.findLockedModelByProblem(problem)
                .orElseThrow(() -> new RuntimeException("Không có model nào được lock cho Problem " + id));

        // Trả về JSON chứa đường dẫn file
        // Ví dụ: { "filePath": "D:/path/to/model.pt" }
        return ResponseEntity.ok(Map.of("filePath", lockedModel.getFilePath()));
    }


    // --- PHẦN 2: GIAO DIỆN ADMIN (THYMELEAF) ---
    // (Đây là phần nhóm bạn sẽ dùng để quản lý model)

    /**
     * Hiển thị trang quản lý chính, liệt kê các bài toán con.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("problems", problemDAO.findAll());
        return "index"; // Trả về file index.html
    }

    /**
     * Hiển thị chi tiết một bài toán con và danh sách các model của nó.
     */
    @GetMapping("/problem/{id}")
    public String viewProblem(@PathVariable Long id, Model model) {
        Problem problem = problemDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Problem " + id));
        
        List<ManagedModel> models = modelDAO.findByProblem(problem);
        
        model.addAttribute("problem", problem);
        model.addAttribute("models", models);
        model.addAttribute("newModel", new ManagedModel()); // Để dùng cho form "Thêm mới"
        return "problem-detail"; // Trả về file problem-detail.html
    }

    /**
     * Xử lý việc thêm một model mới
     */
    @PostMapping("/model/add")
    public String addModel(@ModelAttribute ManagedModel newModel, 
                           @RequestParam Long problemId, 
                           RedirectAttributes redirectAttributes) {
        
        Problem problem = problemDAO.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Problem " + problemId));
        
        newModel.setProblem(problem);
        newModel.setLocked(false); // Mặc định là không lock
        modelDAO.save(newModel);
        
        redirectAttributes.addFlashAttribute("message", "Đã thêm model thành công!");
        return "redirect:/problem/" + problemId;
    }

    /**
     * Xử lý logic "LOCK" model (Đây là logic quan trọng nhất)
     */
    @PostMapping("/model/lock/{modelId}")
    public String lockModel(@PathVariable Long modelId, RedirectAttributes redirectAttributes) {
        ManagedModel modelToLock = modelDAO.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Model " + modelId));
        
        Problem problem = modelToLock.getProblem();

        // Sử dụng method lockModel của DAO (đã tích hợp logic unlock + lock)
        modelDAO.lockModel(modelId);

        redirectAttributes.addFlashAttribute("message", 
            "Đã kích hoạt (lock) model: " + modelToLock.getName());
        return "redirect:/problem/" + problem.getId();
    }
}