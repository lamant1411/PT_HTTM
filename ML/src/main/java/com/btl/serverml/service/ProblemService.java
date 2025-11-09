package com.btl.serverml.service;

import com.btl.serverml.dao.ProblemDAO;
import com.btl.serverml.entity.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProblemService {

    @Autowired
    private ProblemDAO problemDAO;

    // Lấy tất cả problems
    public List<Problem> getAllProblems() {
        return problemDAO.findAll();
    }

    // Lấy problem theo id
    public Problem getProblemById(Long id) {
        return problemDAO.findById(id);
    }

    // Tạo mới problem
    public boolean createProblem(Problem problem) {
        // Validate
        if (problem.getName() == null || problem.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên bài toán không được để trống");
        }
        
        int result = problemDAO.save(problem);
        return result > 0;
    }

    // Cập nhật problem
    public boolean updateProblem(Long id, Problem problem) {
        // Kiểm tra problem có tồn tại không
        if (!problemDAO.exists(id)) {
            throw new IllegalArgumentException("Không tìm thấy bài toán với id: " + id);
        }

        // Validate
        if (problem.getName() == null || problem.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên bài toán không được để trống");
        }

        problem.setId(id);
        int result = problemDAO.update(problem);
        return result > 0;
    }

    // Xóa problem
    public boolean deleteProblem(Long id) {
        // Kiểm tra problem có tồn tại không
        if (!problemDAO.exists(id)) {
            throw new IllegalArgumentException("Không tìm thấy bài toán với id: " + id);
        }

        int result = problemDAO.delete(id);
        return result > 0;
    }

    // Kiểm tra problem có tồn tại không
    public boolean problemExists(Long id) {
        return problemDAO.exists(id);
    }
}
