package com.btl.serverml.service;

import com.btl.serverml.dao.ManagedModelDAO;
import com.btl.serverml.dao.ProblemDAO;
import com.btl.serverml.entity.ManagedModel;
import com.btl.serverml.entity.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Service layer cho quản lý Model và Problem
 */
@Service
public class ModelManagementService {

    @Autowired
    private ProblemDAO problemDAO;

    @Autowired
    private ManagedModelDAO modelDAO;

    /**
     * Lấy đường dẫn model đang được lock cho một problem
     */
    public String getLockedModelPath(Long problemId) {
        Optional<ManagedModel> model = modelDAO.findLockedModelByProblemId(problemId);
        if (model.isPresent()) {
            return model.get().getFilePath();
        }
        throw new RuntimeException("Không tìm thấy model được lock cho Problem #" + problemId);
    }

    /**
     * Tạo Problem mới với validation
     */
    @Transactional
    public Problem createProblem(String name, String description) {
        if (problemDAO.existsByName(name)) {
            throw new RuntimeException("Problem với tên này đã tồn tại: " + name);
        }
        
        Problem problem = new Problem();
        problem.setName(name);
        return problemDAO.save(problem);
    }

    /**
     * Thêm model mới cho một problem
     */
    @Transactional
    public ManagedModel addModel(Long problemId, String name, String description, String filePath) {
        Problem problem = problemDAO.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem không tồn tại: " + problemId));
        
        ManagedModel model = new ManagedModel();
        model.setName(name);
        model.setDescription(description);
        model.setFilePath(filePath);
        model.setProblem(problem);
        model.setLocked(false);
        
        return modelDAO.save(model);
    }

    /**
     * Lock một model và tự động unlock các model khác
     */
    @Transactional
    public void switchLockedModel(Long modelId) {
        modelDAO.lockModel(modelId);
    }

    /**
     * Lấy tất cả model của một problem
     */
    public List<ManagedModel> getModelsByProblem(Long problemId) {
        return modelDAO.findByProblemId(problemId);
    }

    /**
     * Xóa model (chỉ nếu không đang được lock)
     */
    @Transactional
    public boolean deleteModel(Long modelId) {
        Optional<ManagedModel> modelOpt = modelDAO.findById(modelId);
        if (modelOpt.isEmpty()) {
            return false;
        }
        
        ManagedModel model = modelOpt.get();
        if (model.isLocked()) {
            throw new RuntimeException("Không thể xóa model đang được sử dụng (locked)");
        }
        
        return modelDAO.deleteById(modelId);
    }

    /**
     * Lấy thống kê
     */
    public ModelStats getStatistics() {
        Long totalProblems = problemDAO.count();
        Long totalModels = (long) modelDAO.findAll().size();
        
        // Đếm số model đang được lock
        long lockedModels = modelDAO.findAll().stream()
                .filter(ManagedModel::isLocked)
                .count();
        
        return new ModelStats(totalProblems, totalModels, lockedModels);
    }

    /**
     * Inner class cho statistics
     */
    public static class ModelStats {
        public Long totalProblems;
        public Long totalModels;
        public Long lockedModels;

        public ModelStats(Long totalProblems, Long totalModels, Long lockedModels) {
            this.totalProblems = totalProblems;
            this.totalModels = totalModels;
            this.lockedModels = lockedModels;
        }
    }
}
