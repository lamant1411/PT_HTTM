package com.btl.serverml.dao;

import com.btl.serverml.entity.ManagedModel;
import com.btl.serverml.entity.Problem;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho ManagedModel
 */
@Component
public class ManagedModelDAO {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lưu hoặc cập nhật Model
     */
    @Transactional
    public ManagedModel save(ManagedModel model) {
        if (model.getId() == null) {
            entityManager.persist(model);
            return model;
        } else {
            return entityManager.merge(model);
        }
    }

    /**
     * Tìm Model theo ID
     */
    public Optional<ManagedModel> findById(Long id) {
        ManagedModel model = entityManager.find(ManagedModel.class, id);
        return Optional.ofNullable(model);
    }

    /**
     * Lấy tất cả Model
     */
    public List<ManagedModel> findAll() {
        TypedQuery<ManagedModel> query = entityManager.createQuery(
            "SELECT m FROM ManagedModel m ORDER BY m.id",
            ManagedModel.class
        );
        return query.getResultList();
    }

    /**
     * Tìm tất cả Model của một Problem
     */
    public List<ManagedModel> findByProblem(Problem problem) {
        TypedQuery<ManagedModel> query = entityManager.createQuery(
            "SELECT m FROM ManagedModel m WHERE m.problem = :problem ORDER BY m.id",
            ManagedModel.class
        );
        query.setParameter("problem", problem);
        return query.getResultList();
    }

    /**
     * Tìm tất cả Model của một Problem theo ID
     */
    public List<ManagedModel> findByProblemId(Long problemId) {
        TypedQuery<ManagedModel> query = entityManager.createQuery(
            "SELECT m FROM ManagedModel m WHERE m.problem.id = :problemId ORDER BY m.id",
            ManagedModel.class
        );
        query.setParameter("problemId", problemId);
        return query.getResultList();
    }

    /**
     * Tìm Model đang được lock cho một Problem
     */
    public Optional<ManagedModel> findLockedModelByProblem(Problem problem) {
        TypedQuery<ManagedModel> query = entityManager.createQuery(
            "SELECT m FROM ManagedModel m WHERE m.problem = :problem AND m.isLocked = true",
            ManagedModel.class
        );
        query.setParameter("problem", problem);
        List<ManagedModel> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Tìm Model đang được lock cho một Problem theo ID
     */
    public Optional<ManagedModel> findLockedModelByProblemId(Long problemId) {
        TypedQuery<ManagedModel> query = entityManager.createQuery(
            "SELECT m FROM ManagedModel m WHERE m.problem.id = :problemId AND m.isLocked = true",
            ManagedModel.class
        );
        query.setParameter("problemId", problemId);
        List<ManagedModel> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Unlock tất cả model của một Problem trừ model được chỉ định
     */
    @Transactional
    public int unlockOtherModels(Problem problem, Long exceptModelId) {
        return entityManager.createQuery(
            "UPDATE ManagedModel m SET m.isLocked = false WHERE m.problem = :problem AND m.id <> :exceptId"
        )
        .setParameter("problem", problem)
        .setParameter("exceptId", exceptModelId)
        .executeUpdate();
    }

    /**
     * Lock một model (và unlock các model khác của cùng Problem)
     */
    @Transactional
    public void lockModel(Long modelId) {
        ManagedModel model = entityManager.find(ManagedModel.class, modelId);
        if (model == null) {
            throw new RuntimeException("Model không tồn tại: " + modelId);
        }
        
        // Unlock tất cả model khác của cùng problem
        unlockOtherModels(model.getProblem(), modelId);
        
        // Lock model này
        model.setLocked(true);
        entityManager.merge(model);
    }

    /**
     * Unlock một model
     */
    @Transactional
    public void unlockModel(Long modelId) {
        ManagedModel model = entityManager.find(ManagedModel.class, modelId);
        if (model != null) {
            model.setLocked(false);
            entityManager.merge(model);
        }
    }

    /**
     * Xóa Model theo ID
     */
    @Transactional
    public boolean deleteById(Long id) {
        ManagedModel model = entityManager.find(ManagedModel.class, id);
        if (model != null) {
            entityManager.remove(model);
            return true;
        }
        return false;
    }

    /**
     * Đếm số Model của một Problem
     */
    public Long countByProblem(Problem problem) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(m) FROM ManagedModel m WHERE m.problem = :problem",
            Long.class
        );
        query.setParameter("problem", problem);
        return query.getSingleResult();
    }

    /**
     * Kiểm tra Model có tồn tại không
     */
    public boolean existsById(Long id) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(m) FROM ManagedModel m WHERE m.id = :id",
            Long.class
        );
        query.setParameter("id", id);
        return query.getSingleResult() > 0;
    }
}
