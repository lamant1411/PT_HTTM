package com.btl.serverapp.dao;

import com.btl.serverapp.entity.ViolationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
/**
 * Data Access Object for ViolationLog
 * Uses pure JDBC to interact with database
 */
@Component
public class ViolationLogDAO {

    @Autowired
    private DataSource dataSource;

    /**
     * Save a new violation
     * @return true if save successful, false if failed
     */
    public Boolean save(ViolationLog log) {
        if (log.getId() == null) {
            // INSERT
            String sql = "INSERT INTO violation_log (plate_num, evidence_url) VALUES (?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, log.getPlateNum());
                pstmt.setString(3, log.getLogDetails());
                int rowsAffected = pstmt.executeUpdate();
                
                // Get the inserted ID
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            log.setId(generatedKeys.getLong(1));
                        }
                    }
                    return true;
                }
                return false;
            } catch (SQLException e) {
                throw new RuntimeException("Error saving violation log", e);
            }
        } else {
            // UPDATE
            String sql = "UPDATE violation_log SET plate_num = ?, evidence_url = ? WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, log.getPlateNum());
                pstmt.setString(2, log.getEvidenceUrl());
                pstmt.setLong(3, log.getId());

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Error updating violation log", e);
            }
        }
    }
}
