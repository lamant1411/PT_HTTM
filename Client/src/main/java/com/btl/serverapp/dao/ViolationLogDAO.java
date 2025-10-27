package com.btl.serverapp.dao;

import com.btl.serverapp.entity.ViolationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
/**
 * Data Access Object cho ViolationLog
 * Sử dụng JDBC thuần túy để tương tác với database
 */
@Component
public class ViolationLogDAO {

    @Autowired
    private DataSource dataSource;

    /**
     * Lưu một vi phạm mới
     * @return true nếu lưu thành công, false nếu thất bại
     */
    public Boolean save(ViolationLog log) {
        if (log.getId() == null) {
            // INSERT
            String sql = "INSERT INTO violation_log (plate_num, timestamp, evidence_url, log_details) VALUES (?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setString(1, log.getPlateNum());
                pstmt.setTimestamp(2, Timestamp.valueOf(log.getTimestamp()));
                pstmt.setString(3, log.getEvidenceUrl());
                pstmt.setString(4, log.getLogDetails());
                int rowsAffected = pstmt.executeUpdate();
                
                // Lấy ID vừa insert
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
            String sql = "UPDATE violation_log SET plate_num = ?, timestamp = ?, evidence_url = ?, log_details = ? WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, log.getPlateNum());
                pstmt.setTimestamp(2, Timestamp.valueOf(log.getTimestamp()));
                pstmt.setString(3, log.getEvidenceUrl());
                pstmt.setString(4, log.getLogDetails());
                pstmt.setLong(5, log.getId());
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Error updating violation log", e);
            }
        }
    }
}
