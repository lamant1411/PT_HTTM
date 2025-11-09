package com.btl.serverml.dao;

import com.btl.serverml.entity.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProblemDAO {

    @Autowired
    private DataSource dataSource; // Dùng dataSource để tránh việc xung đột khi nhiều kết nối cùng lúc

    // Lấy tất cả problems
    public List<Problem> findAll() {
        String sql = "SELECT * FROM tblproblem ORDER BY id ASC";
        List<Problem> problems = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Problem problem = new Problem();
                problem.setId(rs.getLong("id"));
                problem.setName(rs.getString("name"));
                problem.setDescription(rs.getString("description"));
                problems.add(problem);
            }
            return problems;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all problems", e);
        }
    }

    // Tìm problem theo id
    public Problem findById(Long id) {
        String sql = "SELECT * FROM tblproblem WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Problem problem = new Problem();
                    problem.setId(rs.getLong("id"));
                    problem.setName(rs.getString("name"));
                    problem.setDescription(rs.getString("description"));
                    return problem;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding problem by id: " + id, e);
        }
    }

    // Thêm mới problem
    public int save(Problem problem) {
        String sql = "INSERT INTO tblproblem (name, description) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, problem.getName());
            pstmt.setString(2, problem.getDescription());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        problem.setId(generatedKeys.getLong(1));
                    }
                }
            }
            return rowsAffected;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving problem", e);
        }
    }

    // Cập nhật problem
    public int update(Problem problem) {
        String sql = "UPDATE tblproblem SET name = ?, description = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, problem.getName());
            pstmt.setString(2, problem.getDescription());
            pstmt.setLong(3, problem.getId());
            
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating problem", e);
        }
    }

    // Xóa problem
    public int delete(Long id) {
        String sql = "DELETE FROM tblproblem WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting problem", e);
        }
    }

    // Kiểm tra problem có tồn tại không
    public boolean exists(Long id) {
        String sql = "SELECT COUNT(*) FROM tblproblem WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking if problem exists", e);
        }
    }
}
