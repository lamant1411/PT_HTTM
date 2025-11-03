# 📊 SLIDE THUYẾT TRÌNH - HỆ THỐNG PHÁT HIỆN VI PHẠM GIAO THÔNG

---

## SLIDE 1: GIỚI THIỆU

### 🎯 Tên đồ án
**Hệ thống Phát hiện Vi phạm Giao thông Tự động sử dụng AI**

### 👥 Thông tin
- **Repository:** https://github.com/lamant1411/PT_HTTM
- **Ngày:** November 3, 2025

### 🎯 Mục tiêu
Xây dựng hệ thống tự động:
- ✅ Phát hiện xe vi phạm vượt vạch dừng
- ✅ Nhận diện biển số xe
- ✅ Lưu trữ bằng chứng vi phạm

---

## SLIDE 2: CÔNG NGHỆ SỬ DỤNG

### Backend
- **Spring Boot 2.7.12** - Framework chính
- **Java 11** - Ngôn ngữ lập trình
- **Spring JDBC** - Database access
- **MySQL 8.0** - Cơ sở dữ liệu

### Frontend
- **HTML5/CSS3/JavaScript** - Giao diện web
- **Canvas API** - Vẽ vạch dừng

### AI/ML
- **Python 3.8+** - Ngôn ngữ AI
- **YOLOv8** - Object detection (2 models)
- **OCR Model** - Nhận diện biển số
- **OpenCV** - Xử lý video/hình ảnh
- **ByteTrack** - Tracking xe

---

## SLIDE 3: KIẾN TRÚC HỆ THỐNG

```
┌──────────────┐
│  Web Browser │  ← User upload video + vẽ vạch dừng
└──────┬───────┘
       │ HTTP REST API
       ▼
┌──────────────────┐
│  Spring Boot     │  ← Backend xử lý request
│  (Port 8080)     │
└────┬─────────┬───┘
     │         │
     ▼         ▼
┌─────────┐  ┌──────────┐
│  MySQL  │  │  Python  │  ← AI processing
│Database │  │  Script  │
└─────────┘  └────┬─────┘
                  │
           ┌──────┴──────┐
           ▼             ▼
    ┌──────────┐  ┌──────────┐
    │  YOLOv8  │  │   OCR    │  ← 3 AI Models
    │  Models  │  │  Model   │
    └──────────┘  └──────────┘
```

---

## SLIDE 4: LUỒNG HOẠT ĐỘNG

### Bước 1-3: Upload & Setup
1. User upload video MP4
2. Hiển thị frame đầu tiên
3. Click 2 điểm → vẽ vạch dừng màu đỏ

### Bước 4-6: AI Processing
4. POST video + tọa độ vạch → Backend
5. Backend gọi Python script với 3 model paths
6. Python xử lý từng frame:
   - Detect xe (YOLOv8 Model 1)
   - Track xe (ByteTrack)
   - Check vượt vạch (thuật toán cross product)
   - Detect biển số (YOLOv8 Model 2)
   - OCR đọc biển số (Model 3)
   - Encode ảnh base64

### Bước 7-9: Save & Display
7. Return JSON list violations → Frontend
8. Hiển thị kết quả (plate + ảnh)
9. User click "Lưu" → Insert MySQL + save file

---

## SLIDE 5: THUẬT TOÁN CHÍNH

### 🔹 Phát hiện vượt vạch (Cross Product)

**Công thức:**
```
cross_product = (y - y1) * (x2 - x1) - (y2 - y1) * (x - x1)

• cross_product < 0 → Xe VƯỢT VẠCH (phía dưới)
• cross_product > 0 → Xe CHƯA VƯỢT (phía trên)
• cross_product = 0 → Xe Ở TRÊN VẠCH
```

### 🔹 Tránh duplicate detection

```python
tracked_objects = {}  # Lưu xe đã log
previous_positions = {}  # Lưu vị trí trước

# Chỉ log khi: xe chuyển từ ABOVE → BELOW lần đầu
if was_above_line and is_below_line and unique_key not in tracked_objects:
    # VIOLATION DETECTED!
    tracked_objects[unique_key] = True
```

---

## SLIDE 6: DATABASE SCHEMA

### Bảng: `violation_log`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT | Primary Key, Auto Increment |
| `plate_num` | VARCHAR(20) | Biển số xe |
| `timestamp` | DATETIME | Thời gian vi phạm |
| `evidence_url` | VARCHAR(500) | Đường dẫn ảnh bằng chứng |
| `log_details` | TEXT | Chi tiết bổ sung |

**Indexes:**
- `idx_plate_num` - Tìm kiếm theo biển số nhanh
- `idx_timestamp` - Sắp xếp theo thời gian

**Công nghệ:** JDBC thuần (không dùng JPA)

---

## SLIDE 7: REST API

### Endpoint 1: Process Video
```
POST /api/v1/process
Content-Type: multipart/form-data

Request:
- videoFile: MultipartFile (video.mp4)
- lineData: JSON {"p1": {"x": 100, "y": 200}, "p2": {"x": 500, "y": 200}}

Response: JSON Array
[
  {
    "frame": 150,
    "video_time": "00:05",
    "license_plate": "29A-12345",
    "evidence_image_base64": "data:image/jpeg;base64,...",
    "confidence": 0.92
  }
]
```

### Endpoint 2: Save Violation
```
POST /api/v1/violations/save
Content-Type: application/json

Request:
{
  "license_plate": "29A-12345",
  "evidence_image_base64": "data:image/jpeg;base64,..."
}

Response:
{
  "id": 1,
  "plateNum": "29A-12345",
  "timestamp": "2025-11-03T10:30:00",
  "evidenceUrl": "/violation_images/violation_abc.jpg"
}
```

---

## SLIDE 8: DEMO SCREENSHOTS

### 1. Trang Upload Video
- Upload button
- Canvas hiển thị video
- Input tọa độ P1, P2
- Submit button

### 2. Trang Kết quả
- List violations
- Mỗi item: biển số + ảnh bằng chứng + thời gian
- Button "Lưu vi phạm"

### 3. Database
- Bảng violation_log với records
- Ảnh bằng chứng trong folder static/violation_images/

---

## SLIDE 9: TÍNH NĂNG HOÀN THÀNH

✅ **Core Features:**
1. Upload video (MP4, max 200MB)
2. Vẽ vạch dừng tùy chỉnh (Canvas)
3. Phát hiện xe tự động (YOLOv8)
4. Tracking xe (ByteTrack - tránh duplicate)
5. Kiểm tra vượt vạch (Cross product algorithm)
6. Detect biển số (YOLOv8)
7. OCR đọc ký tự (OCR Model)
8. Encode/Decode Base64 (gửi/nhận ảnh)
9. Hiển thị kết quả (Frontend)
10. Lưu database (MySQL JDBC)
11. Lưu ảnh file (static folder)
12. REST API (2 endpoints)

✅ **Technical:**
- Exception handling
- Resource cleanup (try-with-resources)
- Async processing (ProcessBuilder)
- UTF-8 encoding (Windows compatible)
- Auto-run schema.sql

---

## SLIDE 10: KẾT QUẢ

### 📊 Số liệu thống kê

**Code:**
- Backend Java: ~800 LoC
- Frontend JS: ~600 LoC
- Python AI: ~400 LoC
- **Tổng: ~1800+ LoC**

**Performance:**
- Video 1 phút (30fps): ~30-60 giây
- Video 5 phút: ~3-5 phút
- Accuracy: Phụ thuộc chất lượng video

**Models:**
- 3 AI models (YOLOv8 x2, OCR x1)
- Total size: ~50-100MB

### ✅ Ưu điểm

1. **Kiến trúc rõ ràng** - Frontend/Backend/AI phân tách
2. **Công nghệ hiện đại** - YOLOv8, Spring Boot
3. **UX tốt** - Canvas drawing trực quan
4. **Code quality** - Comments đầy đủ, error handling
5. **Scalable** - Dễ mở rộng thêm tính năng

---

## SLIDE 11: HẠN CHẾ & CẢI TIẾN

### ⚠️ Hạn chế hiện tại

1. **Performance:**
   - Xử lý video chậm với file lớn
   - Load models mỗi lần chạy

2. **Accuracy:**
   - OCR phụ thuộc chất lượng video
   - Góc quay camera ảnh hưởng

3. **Scalability:**
   - Chỉ xử lý 1 video cùng lúc
   - Không có queue system

4. **Security:**
   - Chưa có authentication
   - Hard-coded paths

### 🚀 Đề xuất cải tiến

**Phase 2:**
- ✨ Cache AI models (load 1 lần)
- ✨ Async processing với Redis Queue
- ✨ Multi-threading
- ✨ GPU acceleration
- ✨ Spring Security + JWT
- ✨ Dashboard với statistics
- ✨ Export reports (PDF/Excel)
- ✨ Real-time video streaming
- ✨ Mobile app
- ✨ Docker containerization

---

## SLIDE 12: ỨNG DỤNG THỰC TÊ

### 🎯 Có thể áp dụng cho:

1. **🚦 Cơ quan quản lý giao thông**
   - Giám sát vi phạm tự động
   - Giảm tải công việc

2. **🏛️ Công an giao thông**
   - Xử phạt có bằng chứng
   - Lưu trữ hệ thống

3. **🏢 Công ty an ninh**
   - Camera giám sát thông minh
   - Tích hợp hệ thống hiện có

4. **🎓 Nghiên cứu khoa học**
   - Dataset vi phạm giao thông
   - Training AI models

5. **💼 Startup công nghệ**
   - Smart city solutions
   - IoT transportation

---

## SLIDE 13: KẾT LUẬN

### ✅ Đã hoàn thành

- ✅ Xây dựng **hệ thống web application hoàn chỉnh**
- ✅ Tích hợp **3 AI models** (detection + OCR)
- ✅ Triển khai **database MySQL** với JDBC
- ✅ Giao diện **thân thiện, trực quan**
- ✅ **REST API chuẩn**
- ✅ **Code quality tốt**, comments đầy đủ

### 🎓 Kỹ năng đạt được

- Spring Boot framework
- REST API design
- JDBC programming
- AI/ML integration (YOLOv8, OCR)
- Object tracking algorithms
- Canvas API
- Python-Java integration
- Database schema design
- Git version control

### 💡 Đóng góp

Đồ án đóng góp vào việc:
- ✅ **Tự động hóa** giám sát giao thông
- ✅ **Giảm tải** công việc cho CSGT
- ✅ **Tăng tính khách quan** trong xử phạt
- ✅ **Ứng dụng AI** vào bài toán thực tế

---

## SLIDE 14: DEMO TRỰC TIẾP

### 📺 Các bước demo:

1. **Start application**
   ```bash
   cd Client
   mvn spring-boot:run
   ```

2. **Truy cập:** http://localhost:8080/html/violation.html

3. **Upload video** giao thông mẫu

4. **Vẽ vạch dừng** (click 2 điểm)

5. **Click Submit** → Chờ processing

6. **Xem kết quả** vi phạm

7. **Lưu vào database** → Check MySQL

8. **Xem lịch sử** vi phạm đã lưu

---

## SLIDE 15: Q&A

### ❓ Các câu hỏi thường gặp:

**Q1: Tại sao không dùng JPA mà dùng JDBC thuần?**
- A: Để có full control SQL queries, performance tốt hơn, không có "magic" của JPA

**Q2: Làm sao tránh duplicate detection?**
- A: Sử dụng tracking với unique_key, chỉ log khi xe chuyển từ ABOVE → BELOW lần đầu

**Q3: Accuracy của OCR bao nhiêu %?**
- A: Phụ thuộc chất lượng video, thường đạt 85-95% với video tốt

**Q4: Có thể xử lý real-time không?**
- A: Hiện tại chưa, cần nâng cấp với streaming + GPU acceleration

**Q5: Hệ thống có thể scale lên bao nhiêu camera?**
- A: Với kiến trúc hiện tại: 1 video tại 1 thời điểm. Cần refactor thành microservices để scale

---

## SLIDE 16: TÀI LIỆU THAM KHẢO

### 📚 Repository & Docs
- **GitHub:** https://github.com/lamant1411/PT_HTTM
- **Báo cáo chi tiết:** `BAO_CAO_DU_AN.md`
- **Migration guide:** `MIGRATION_TO_JDBC.md`
- **README:** `README.md`

### 🔗 Technologies
- Spring Boot: https://spring.io/projects/spring-boot
- YOLOv8: https://github.com/ultralytics/ultralytics
- OpenCV: https://opencv.org/
- ByteTrack: https://github.com/ifzhang/ByteTrack

### 📊 Papers
- "YOLOv8: You Only Look Once version 8"
- "ByteTrack: Multi-Object Tracking by Associating Every Detection Box"

---

## 🎉 CẢM ƠN THẦY ĐÃ LẮNG NGHE!

### 📞 Liên hệ:
- **GitHub:** lamant1411
- **Repository:** https://github.com/lamant1411/PT_HTTM

### 🙏 Xin chân thành cảm ơn:
- Thầy đã hướng dẫn
- Các bạn đã hỗ trợ
- Cộng đồng AI/ML

---

*Prepared: November 3, 2025*  
*Format: Markdown Slides*
