package com.btl.serverml.dao;

import com.btl.serverml.entity.Problem;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho Problem
 */
@Component
public class ProblemDAO {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lưu hoặc cập nhật Problem
     */
    @Transactional
    public Problem save(Problem problem) {
        if (problem.getId() == null) {
            entityManager.persist(problem);
            return problem;
        } else {
            return entityManager.merge(problem);
        }
    }

    /**
     * Tìm Problem theo ID
     */
    public Optional<Problem> findById(Long id) {
        Problem problem = entityManager.find(Problem.class, id);
        return Optional.ofNullable(problem);
    }

    /**
     * Tìm Problem theo tên
     */
    public Optional<Problem> findByName(String name) {
        TypedQuery<Problem> query = entityManager.createQuery(
            "SELECT p FROM Problem p WHERE p.name = :name",
            Problem.class
        );
        query.setParameter("name", name);
        List<Problem> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lấy tất cả Problem
     */
    public List<Problem> findAll() {
        TypedQuery<Problem> query = entityManager.createQuery(
            "SELECT p FROM Problem p ORDER BY p.id",
            Problem.class
        );
        return query.getResultList();
    }

    /**
     * Đếm tổng số Problem
     */
    public Long count() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Problem p",
            Long.class
        );
        return query.getSingleResult();
    }

    /**
     * Xóa Problem theo ID
     */
    @Transactional
    public boolean deleteById(Long id) {
        Problem problem = entityManager.find(Problem.class, id);
        if (problem != null) {
            entityManager.remove(problem);
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra Problem có tồn tại không
     */
    public boolean existsById(Long id) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Problem p WHERE p.id = :id",
            Long.class
        );
        query.setParameter("id", id);
        return query.getSingleResult() > 0;
    }

    /**
     * Kiểm tra tên Problem đã tồn tại chưa
     */
    public boolean existsByName(String name) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(p) FROM Problem p WHERE p.name = :name",
            Long.class
        );
        query.setParameter("name", name);
        return query.getSingleResult() > 0;
    }
}
