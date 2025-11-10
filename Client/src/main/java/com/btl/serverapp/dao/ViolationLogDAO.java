package com.btl.serverapp.dao;

import com.btl.serverapp.entity.ViolationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
@Component
public class ViolationLogDAO {

    @Autowired
    private DataSource dataSource; //Dùng dataSource để tránh việc xung đột khi nhiều kết nối cùng lúc

    /**
     * @return
     */
    public Boolean save(ViolationLog log) {
        String sql = "INSERT INTO tblviolation_log (plate_num, evidence_url, plateImageUrl) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, log.getPlateNum());
            pstmt.setString(2, log.getEvidenceUrl());
            pstmt.setString(3, log.getPlateImageUrl());
            
            int rowsAffected = pstmt.executeUpdate();
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
    }
}
