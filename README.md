# Hệ thống Phát hiện Vi phạm Giao thông

## 🎯 Giới thiệu

Hệ thống tự động phát hiện xe vi phạm vượt vạch dừng sử dụng AI (YOLOv8, OCR).

## 🏗️ Kiến trúc

```
Browser → Client Server (8080) → Python Script → AI Models
                ↓
         H2 Database
```

## 📋 Yêu cầu hệ thống

- **Java 11+** và **Maven 3.6+**
- **Python 3.8+**
- **Git**

## ⚙️ Cài đặt

### 1. Clone repository

```bash
git clone <repository-url>
cd PT_HTTM
```

### 2. Cài đặt Python dependencies

```bash
cd ML/Model_Scripts
pip install -r requirements.txt
```

### 3. Cấu hình đường dẫn

Sửa file `Client/src/main/resources/application.properties`:

```properties
# Đường dẫn Python script
python.script.path=<ABSOLUTE_PATH>/ML/Model_Scripts/process.py

# Đường dẫn models
model.path.object-detect=<ABSOLUTE_PATH>/ML/Model_Scripts/models/yolov8_detect_xe.pt
model.path.plate-detect=<ABSOLUTE_PATH>/ML/Model_Scripts/models/yolov8_detect_bienso.pt
model.path.ocr=<ABSOLUTE_PATH>/ML/Model_Scripts/models/ocr_net.pt
```

Sửa file `ML/Model_Scripts/process.py`:

```python
STATIC_IMAGE_SAVE_PATH = "<ABSOLUTE_PATH>/Client/src/main/resources/static/violation_images/"
```

**⚠️ Lưu ý:** Phải dùng đường dẫn TUYỆT ĐỐI!

### 4. Chạy ứng dụng

```bash
cd Client
mvn spring-boot:run
```

Server chạy tại: **http://localhost:8080**

## 🚀 Sử dụng

1. Truy cập: http://localhost:8080/html/violation.html
2. Upload video
3. Vẽ vạch dừng bằng cách click 2 điểm
4. Nhấn "Phát hiện vi phạm"
5. Xem kết quả và lưu vào database

## 📊 Database

- H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:clientdb`
- Username: `sa`
- Password: (để trống)

## 🔧 Troubleshooting

### Lỗi "Python script not found"
→ Check đường dẫn trong `application.properties` (phải tuyệt đối)

### Lỗi "Cannot delete temp file"
→ Đã fix với Thread.sleep(500ms)

### Lỗi "Image not found"
→ Check `STATIC_IMAGE_SAVE_PATH` trong `process.py`

## 📁 Cấu trúc dự án

```
PT_HTTM/
├── Client/           # Spring Boot server (Port 8080)
│   ├── src/main/
│   │   ├── java/     # Backend code
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/  # Frontend (HTML/JS/CSS)
│   └── pom.xml
│
└── ML/
    └── Model_Scripts/
        ├── process.py      # Python AI script
        ├── requirements.txt
        └── models/         # YOLOv8 models (.pt)
```

## 🎓 Luồng xử lý (10 bước)

1. User upload video + vẽ vạch dừng
2. Client Server nhận request
3. Lưu file tạm (temp/)
4. Gọi Python script qua ProcessBuilder
5. Python xử lý video (load models → detect → track → OCR)
6. Python trả JSON kết quả
7. Client parse JSON
8. Trả kết quả về Frontend
9. Hiển thị kết quả trên giao diện
10. User chọn lưu/xóa vi phạm vào DB

## 🛠️ Tech Stack

- **Backend:** Spring Boot 2.7.12, Java 11, JDBC
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **AI:** Python 3, YOLOv8, OpenCV
- **Database:** H2 in-memory
- **Integration:** ProcessBuilder

## 📝 Chi tiết

Xem file [BAO_CAO_DU_AN.md](BAO_CAO_DU_AN.md) để biết thêm chi tiết về:
- Kiến trúc hệ thống
- Code documentation
- API endpoints
- Database schema
- Hướng dẫn phát triển

---

**Ngày cập nhật:** 2/11/2025
