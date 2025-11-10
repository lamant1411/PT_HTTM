-- Script cập nhật database để thêm cột lưu ảnh biển số xe
-- Chạy script này trên database: pt_httm
-- Người tạo: Agent
-- Ngày: 2025-11-10
-- Lưu ý: Không có timestamp và log_details, tên cột là plateImageUrl

USE pt_httm;

-- Kiểm tra cấu trúc bảng hiện tại
DESCRIBE tblviolation_log;

-- Thêm cột plateImageUrl vào bảng tblviolation_log nếu chưa có
ALTER TABLE tblviolation_log 
ADD COLUMN plateImageUrl VARCHAR(255) DEFAULT NULL 
COMMENT 'Đường dẫn đến ảnh biển số xe đã được crop';

-- Xác nhận cột đã được thêm
DESCRIBE tblviolation_log;

-- Hiển thị dữ liệu mẫu để kiểm tra
SELECT id, plate_num, evidence_url, plateImageUrl 
FROM tblviolation_log 
LIMIT 5;
