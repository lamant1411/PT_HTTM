# 📋 TÓM TẮT DỰ ÁN - 1 TRANG

## HỆ THỐNG PHÁT HIỆN VI PHẠM GIAO THÔNG TỰ ĐỘNG

**Repository:** https://github.com/lamant1411/PT_HTTM  
**Ngày:** November 3, 2025

---

## 🎯 MỤC TIÊU
Xây dựng hệ thống tự động phát hiện xe vi phạm vượt vạch dừng bằng AI (YOLOv8 + OCR)

## 🏗️ KIẾN TRÚC
```
Browser → Spring Boot (8080) → Python AI Script → 3 Models (YOLOv8 + OCR) → MySQL
```

## 💻 CÔNG NGHỆ

| Layer | Tech Stack |
|-------|-----------|
| **Backend** | Spring Boot 2.7.12 + Java 11 + Spring JDBC + MySQL 8.0 |
| **Frontend** | HTML5 + CSS3 + JavaScript + Canvas API |
| **AI/ML** | Python 3.8 + YOLOv8 + OpenCV + ByteTrack + OCR |

## 📊 DATABASE
**Bảng:** `violation_log` (id, plate_num, timestamp, evidence_url, log_details)  
**Công nghệ:** JDBC thuần (không JPA)

## 🔄 LUỒNG HOẠT ĐỘNG
1. User upload video + vẽ vạch dừng (Canvas)
2. POST → Backend → Python script
3. AI xử lý: Detect xe → Track → Check vượt vạch → OCR biển số
4. Return JSON → Frontend hiển thị
5. User confirm → Save MySQL + file

## 🎯 TÍNH NĂNG (12/12 ✅)
✅ Upload video  
✅ Vẽ vạch dừng  
✅ Phát hiện xe (YOLOv8)  
✅ Tracking (ByteTrack)  
✅ Kiểm tra vượt vạch  
✅ Detect biển số  
✅ OCR đọc ký tự  
✅ Encode/Decode Base64  
✅ Hiển thị kết quả  
✅ Lưu database  
✅ Lưu ảnh file  
✅ REST API (2 endpoints)

## 📈 KẾT QUẢ
- **Code:** ~1800+ LoC (Java 800 + JS 600 + Python 400)
- **Models:** 3 AI models (~50-100MB)
- **Performance:** 1 phút video → 30-60s xử lý
- **API:** 2 endpoints (POST /api/v1/process, /api/v1/violations/save)

## ✅ ƯU ĐIỂM
- Kiến trúc rõ ràng (Frontend/Backend/AI phân tách)
- Công nghệ hiện đại (YOLOv8 state-of-the-art)
- UX tốt (Canvas drawing trực quan)
- Code quality (Comments, error handling)
- JDBC thuần (full control SQL)

## ⚠️ HẠN CHẾ
- Xử lý chậm với video lớn (chưa có queue/async)
- Chỉ 1 video cùng lúc
- OCR phụ thuộc chất lượng video
- Chưa có authentication

## 🚀 CẢI TIẾN
Phase 2: Cache models, Redis Queue, Multi-threading, GPU, Spring Security, Dashboard, Export reports, Real-time streaming, Mobile app, Docker

## 🎓 KỸ NĂNG ĐẠT ĐƯỢC
Spring Boot • REST API • JDBC • AI Integration • YOLOv8 • Object Tracking • Canvas API • Python-Java • Database Design • Git

## 🎯 ỨNG DỤNG
Cơ quan giao thông • Công an CSGT • Công ty an ninh • Nghiên cứu khoa học • Startup Smart City

## 📁 CẤU TRÚC
```
PT_HTTM/
├── Client/                    # Spring Boot (Java)
│   ├── src/main/java/        # Controller, Service, DAO, Entity
│   └── src/main/resources/   # application.properties, schema.sql, static/
└── ML/Model_Scripts/         # Python AI
    ├── process.py            # Main script
    └── models/               # 3 models (.pt files)
```

## 🔗 TÀI LIỆU
- **Báo cáo chi tiết:** `BAO_CAO_DU_AN.md` (13 pages)
- **Slide thuyết trình:** `SLIDE_THUYET_TRINH.md` (16 slides)
- **Migration guide:** `MIGRATION_TO_JDBC.md`
- **README:** Hướng dẫn cài đặt

## 🚀 CHẠY DỰ ÁN
```bash
# 1. Cài Python packages
cd ML/Model_Scripts
pip install ultralytics opencv-python

# 2. Tạo database
mysql> CREATE DATABASE pt_httm;

# 3. Sửa config (application.properties, process.py)

# 4. Run
cd Client
mvn spring-boot:run

# 5. Access
http://localhost:8080/html/violation.html
```

---

**✨ Hệ thống hoàn chỉnh, sẵn sàng demo!**
