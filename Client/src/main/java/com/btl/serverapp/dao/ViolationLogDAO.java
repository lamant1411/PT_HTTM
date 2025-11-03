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
     * Save a new violation using pure JDBC
     * @return true if save successful, false if failed
     */
    public Boolean save(ViolationLog log) {
        if (log.getId() == 0) {
            // INSERT
            String sql = "INSERT INTO violation_log (plate_num, timestamp, evidence_url, log_details) VALUES (?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, log.getPlateNum());
                pstmt.setTimestamp(2, log.getTimestamp() != null ? Timestamp.valueOf(log.getTimestamp()) : new Timestamp(System.currentTimeMillis()));
                pstmt.setString(3, log.getEvidenceUrl());
                
                int rowsAffected = pstmt.executeUpdate();
                
                // Get the inserted ID
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            log.setId(generatedKeys.getInt(1));
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
            String sql = "UPDATE violation_log SET plate_num = ?, evidence_url = ?, WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, log.getPlateNum());
                pstmt.setString(3, log.getEvidenceUrl());
                pstmt.setInt(5, log.getId());

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Error updating violation log", e);
            }
        }
    }
    
    /**
     * Find violation by ID
     */
    public ViolationLog findById(Long id) {
        String sql = "SELECT * FROM violation_log WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapRowToViolationLog(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding violation log by ID", e);
        }
    }
    
    /**
     * Find all violations
     */
    public java.util.List<ViolationLog> findAll() {
        String sql = "SELECT * FROM violation_log ORDER BY timestamp DESC";
        java.util.List<ViolationLog> results = new java.util.ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                results.add(mapRowToViolationLog(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all violation logs", e);
        }
    }
    
    /**
     * Map ResultSet row to ViolationLog object
     */
    private ViolationLog mapRowToViolationLog(ResultSet rs) throws SQLException {
        ViolationLog log = new ViolationLog();
        log.setId(rs.getInt("id"));
        log.setPlateNum(rs.getString("plate_num"));
        
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) {
            log.setTimestamp(ts.toLocalDateTime());
        }
        
        log.setEvidenceUrl(rs.getString("evidence_url"));
        return log;
    }
}
