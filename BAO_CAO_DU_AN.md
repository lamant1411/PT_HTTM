# 📋 BÁO CÁO ĐỒ ÁN: HỆ THỐNG PHÁT HIỆN VI PHẠM GIAO THÔNG

---

## 📌 I. THÔNG TIN CHUNG

### 1.1. Tên đồ án
**Hệ thống Phát hiện Vi phạm Giao thông Tự động sử dụng AI**  
*(Traffic Violation Detection System using AI)*

### 1.2. Mục tiêu
Xây dựng hệ thống tự động phát hiện xe vi phạm vượt vạch dừng bằng công nghệ:
- ✅ Computer Vision (OpenCV)
- ✅ Deep Learning (YOLOv8)
- ✅ OCR (Optical Character Recognition)
- ✅ Web Application (Spring Boot)

### 1.3. Phạm vi
- Upload video giao thông
- Vẽ vạch dừng tùy chỉnh
- Phát hiện xe vi phạm tự động
- Nhận diện biển số xe
- Lưu trữ bằng chứng vi phạm

---

## 🏗️ II. KIẾN TRÚC HỆ THỐNG

### 2.1. Kiến trúc tổng quan

```
┌─────────────────┐
│   Web Browser   │ (HTML/CSS/JavaScript)
└────────┬────────┘
         │ HTTP REST API
         ▼
┌─────────────────────┐
│  Spring Boot Server │ (Port 8080)
│  ┌───────────────┐  │
│  │ Controller    │  │ - ViolationController
│  │ Service       │  │ - ViolationService
│  │ DAO (JDBC)    │  │ - ViolationLogDAO
│  │ Entity        │  │ - ViolationLog
│  └───────────────┘  │
└──────┬──────────┬───┘
       │          │
       ▼          ▼
┌──────────┐  ┌─────────────┐
│  MySQL   │  │ Python AI   │
│ Database │  │   Script    │
└──────────┘  └──────┬──────┘
                     │
              ┌──────┴──────┐
              ▼             ▼
       ┌──────────┐  ┌──────────┐
       │  YOLOv8  │  │   OCR    │
       │  Models  │  │  Model   │
       └──────────┘  └──────────┘
```

### 2.2. Công nghệ sử dụng

| Layer | Công nghệ | Phiên bản |
|-------|-----------|-----------|
| **Backend** | Spring Boot | 2.7.12 |
| | Java | 11 |
| | Spring Web | REST API |
| | Spring JDBC | Database Access |
| | Maven | Build Tool |
| **Frontend** | HTML5/CSS3 | - |
| | JavaScript ES6+ | - |
| | Canvas API | Drawing |
| **Database** | MySQL | 8.0+ |
| | JDBC Driver | 8.0.33 |
| **AI/ML** | Python | 3.8+ |
| | YOLOv8 (Ultralytics) | Latest |
| | OpenCV | cv2 |
| | ByteTrack | Tracking |
| **Server** | Embedded Tomcat | Spring Boot |

---

## 📊 III. CƠ SỞ DỮ LIỆU

### 3.1. Database Schema

**Tên Database:** `pt_httm`

**Bảng: `violation_log`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ID vi phạm |
| `plate_num` | VARCHAR(20) | NOT NULL | Biển số xe |
| `timestamp` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Thời gian vi phạm |
| `evidence_url` | VARCHAR(500) | | Đường dẫn ảnh bằng chứng |
| `log_details` | TEXT | | Chi tiết bổ sung (JSON) |

**Indexes:**
- `idx_plate_num` trên cột `plate_num`
- `idx_timestamp` trên cột `timestamp`

### 3.2. File schema.sql
```sql
CREATE TABLE IF NOT EXISTS violation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_num VARCHAR(20) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    evidence_url VARCHAR(500),
    log_details TEXT,
    INDEX idx_plate_num (plate_num),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 🔧 IV. CHI TIẾT TRIỂN KHAI

### 4.1. Backend (Spring Boot)

#### **A. Entity Layer**

**File:** `ViolationLog.java`

```java
/**
 * Plain POJO class representing violation_log table
 * No JPA annotations - pure JDBC approach
 */
public class ViolationLog {
    private int id;
    private String plateNum;
    private LocalDateTime timestamp;
    private String evidenceUrl;
    
    // Constructor tự động set timestamp
    public ViolationLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters...
}
```

**Đặc điểm:**
- ✅ POJO thuần túy (không JPA annotations)
- ✅ Tự động set timestamp khi tạo object
- ✅ Ánh xạ 1-1 với bảng database

#### **B. DAO Layer (Data Access Object)**

**File:** `ViolationLogDAO.java`

```java
@Component
public class ViolationLogDAO {
    @Autowired
    private DataSource dataSource;
    
    // Methods:
    // - save(ViolationLog log): Boolean
    // - findById(Long id): ViolationLog
    // - findAll(): List<ViolationLog>
    // - mapRowToViolationLog(ResultSet rs): ViolationLog
}
```

**Đặc điểm:**
- ✅ Sử dụng JDBC thuần (không JPA Repository)
- ✅ Full control SQL queries
- ✅ PreparedStatement để tránh SQL Injection
- ✅ Auto-close resources với try-with-resources

#### **C. Service Layer**

**File:** `ViolationService.java`

**Chức năng chính:**

1. **processVideo()** - Xử lý video phát hiện vi phạm
   - Lưu video tạm thời
   - Gọi Python script qua ProcessBuilder
   - Đọc kết quả JSON từ stdout
   - Cleanup temp files
   - Return JSON về Controller

2. **saveViolationWithImage()** - Lưu vi phạm + ảnh
   - Decode base64 image
   - Lưu file ảnh vào `static/violation_images/`
   - Tạo entity ViolationLog
   - Gọi DAO để save vào database

**Flow:**
```
Video Upload → Temp Files → Python Script → JSON Result → Frontend
                                                 ↓
                                         User Confirms Save
                                                 ↓
                                  Base64 Image + Plate Number
                                                 ↓
                                      Save to DB + File System
```

#### **D. Controller Layer**

**File:** `ViolationController.java`

**REST API Endpoints:**

| Method | Endpoint | Input | Output | Mô tả |
|--------|----------|-------|--------|-------|
| POST | `/api/v1/process` | MultipartFile video<br>String lineData | JSON array | Xử lý video phát hiện vi phạm |
| POST | `/api/v1/violations/save` | JSON {plate, image} | ViolationLog | Lưu vi phạm vào DB |

**Request/Response Examples:**

```json
// POST /api/v1/process - Response
[
  {
    "frame": 150,
    "video_time": "00:05",
    "license_plate": "29A-12345",
    "evidence_image_base64": "data:image/jpeg;base64,/9j/4AAQ...",
    "confidence": 0.89
  }
]

// POST /api/v1/violations/save - Request
{
  "license_plate": "29A-12345",
  "evidence_image_base64": "data:image/jpeg;base64,/9j/4AAQ..."
}

// POST /api/v1/violations/save - Response
{
  "id": 1,
  "plateNum": "29A-12345",
  "timestamp": "2025-11-03T10:30:00",
  "evidenceUrl": "/violation_images/violation_abc123.jpg"
}
```

### 4.2. AI/ML (Python Script)

#### **File:** `process.py`

**Tham số đầu vào (Command Line):**
```bash
python process.py \
  --video /path/to/video.mp4 \
  --config /path/to/config.json \
  --object_model /path/to/yolov8_detect_xe.pt \
  --plate_model /path/to/yolov8_detect_bienso.pt \
  --ocr_model /path/to/ocr_net.pt
```

**Workflow chi tiết:**

```
┌─────────────────────────────────────────────────────────┐
│ 1. LOAD MODELS                                          │
│    - YOLOv8 Object Detection (xe)                       │
│    - YOLOv8 License Plate Detection                     │
│    - OCR Model (đọc ký tự)                              │
└───────────────────────┬─────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────┐
│ 2. READ CONFIGURATION                                   │
│    - Stop line: P1(x1,y1), P2(x2,y2)                   │
└───────────────────────┬─────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────┐
│ 3. PROCESS VIDEO FRAME BY FRAME                         │
│    For each frame:                                      │
│    ┌───────────────────────────────────────────────┐   │
│    │ 3a. DETECT & TRACK vehicles (ByteTrack)      │   │
│    │     - Get bounding box (x1,y1,x2,y2)         │   │
│    │     - Get track_id, confidence                │   │
│    └──────────────────┬────────────────────────────┘   │
│                       ▼                                 │
│    ┌───────────────────────────────────────────────┐   │
│    │ 3b. CHECK VIOLATION                           │   │
│    │     - Calculate bottom_center of vehicle     │   │
│    │     - Check if crossed stop line             │   │
│    │     - Track previous positions                │   │
│    └──────────────────┬────────────────────────────┘   │
│                       ▼                                 │
│    ┌───────────────────────────────────────────────┐   │
│    │ 3c. IF VIOLATION DETECTED:                    │   │
│    │     - Crop vehicle region                     │   │
│    │     - Detect license plate (Model 2)          │   │
│    │     - Crop plate region                       │   │
│    │     - OCR read text (Model 3)                 │   │
│    │     - Draw bounding box + text                │   │
│    │     - Encode image to base64                  │   │
│    │     - Log violation                           │   │
│    └───────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────┐
│ 4. RETURN JSON RESULT to Java (stdout)                 │
│    [{"frame": 150, "license_plate": "29A-12345", ...}] │
└─────────────────────────────────────────────────────────┘
```

**Thuật toán phát hiện vượt vạch:**

```python
def is_crossing_line(point, p1, p2):
    """
    Công thức: cross_product = (y - y1) * (x2 - x1) - (y2 - y1) * (x - x1)
    
    - cross_product < 0: Điểm ở phía dưới đường thẳng (VƯỢT VẠCH)
    - cross_product > 0: Điểm ở phía trên đường thẳng (CHƯA VƯỢT)
    - cross_product = 0: Điểm nằm trên đường thẳng
    """
    x, y = point
    x1, y1 = p1
    x2, y2 = p2
    cross_product = (y - y1) * (x2 - x1) - (y2 - y1) * (x - x1)
    return cross_product < 0
```

**Tracking logic để tránh duplicate:**

```python
tracked_objects = {}  # {unique_key: True/False}
previous_positions = {}  # {unique_key: (is_above_line, frame)}

# Mỗi frame:
unique_key = f"id_{track_id}" if track_id else f"pos_{int(x//30)}_{int(y//30)}"

# Chỉ log khi: xe chuyển từ ABOVE → BELOW lần đầu tiên
if was_above_line and is_below_line and unique_key not in tracked_objects:
    # VIOLATION DETECTED!
    tracked_objects[unique_key] = True
```

**3 AI Models:**

| Model | Mục đích | Input | Output |
|-------|----------|-------|--------|
| **Model 1**<br>`yolov8_detect_xe.pt` | Phát hiện và tracking xe | Frame (image) | Bounding boxes + track IDs |
| **Model 2**<br>`yolov8_detect_bienso.pt` | Phát hiện vùng biển số | Cropped vehicle image | Bounding box biển số |
| **Model 3**<br>`ocr_net.pt` | Đọc ký tự biển số | Cropped plate image | Text (VD: "29A-12345") |

### 4.3. Frontend (Web Interface)

#### **Cấu trúc files:**

```
static/
├── index.html              # Landing page
├── html/
│   ├── violation.html      # Trang upload video + vẽ vạch
│   ├── results.html        # Hiển thị kết quả vi phạm
│   └── history.html        # Lịch sử vi phạm đã lưu
├── js/
│   ├── violation.app.js    # Logic upload, canvas, API call
│   └── results.app.js      # Hiển thị results, save to DB
├── css/
│   └── violation.style.css # Styling
└── violation_images/       # Thư mục lưu ảnh bằng chứng
```

#### **User Flow:**

```
1. User truy cập: http://localhost:8080/html/violation.html
                                ↓
2. Upload video MP4 → Hiển thị frame đầu tiên trên canvas
                                ↓
3. Click 2 điểm trên canvas → Vẽ vạch dừng (đường màu đỏ)
                                ↓
4. Click "Submit" → POST /api/v1/process
                                ↓
5. Loading... (Python processing)
                                ↓
6. Redirect to results.html → Hiển thị list vi phạm
                                ↓
7. User review → Click "Lưu vi phạm" cho từng item
                                ↓
8. POST /api/v1/violations/save → Lưu vào DB + file system
                                ↓
9. Success notification → Có thể xem lại ở history.html
```

#### **Canvas Drawing Logic:**

**File:** `violation.app.js`

```javascript
// 1. Load video → Vẽ frame đầu tiên lên canvas
const video = document.createElement('video');
video.src = URL.createObjectURL(file);
video.onloadeddata = () => {
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
};

// 2. Click canvas → Lưu tọa độ
canvas.addEventListener('click', (e) => {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    
    const x = (e.clientX - rect.left) * scaleX;
    const y = (e.clientY - rect.top) * scaleY;
    
    points.push({x, y});
    
    // Vẽ điểm đỏ
    ctx.fillStyle = 'red';
    ctx.arc(x, y, 5, 0, 2*Math.PI);
    ctx.fill();
    
    // Nếu đủ 2 điểm → vẽ đường nối
    if (points.length === 2) {
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        ctx.lineTo(points[1].x, points[1].y);
        ctx.strokeStyle = 'red';
        ctx.lineWidth = 3;
        ctx.stroke();
    }
});

// 3. Submit → Gửi video + line coordinates
const formData = new FormData();
formData.append('videoFile', videoFile);
formData.append('lineData', JSON.stringify({
    p1: {x: points[0].x, y: points[0].y},
    p2: {x: points[1].x, y: points[1].y}
}));

fetch('/api/v1/process', {method: 'POST', body: formData})
    .then(res => res.json())
    .then(data => {
        localStorage.setItem('violationResults', JSON.stringify(data));
        window.location.href = 'results.html';
    });
```

---

## 🔄 V. LUỒNG HOẠT ĐỘNG TỔNG THỂ

### 5.1. Use Case: Phát hiện vi phạm từ video

```
┌──────────┐                                              ┌──────────────┐
│  User    │                                              │ Spring Boot  │
└────┬─────┘                                              └──────┬───────┘
     │                                                            │
     │ 1. Upload video + vẽ vạch dừng                            │
     │────────────────────────────────────────────────────────>  │
     │                                                            │
     │                                                            │ 2. Save temp files
     │                                                            │───────────┐
     │                                                            │           │
     │                                                            │<──────────┘
     │                                                            │
     │                                                            │ 3. Call Python script
     │                                                            │───────────────────┐
     │                                                            │                   │
     │                                                ┌───────────▼────────┐          │
     │                                                │  Python AI Script  │          │
     │                                                ├────────────────────┤          │
     │                                                │ 4a. Load 3 models  │          │
     │                                                │ 4b. Process video  │          │
     │                                                │ 4c. Detect + Track │          │
     │                                                │ 4d. Check violation│          │
     │                                                │ 4e. OCR plate      │          │
     │                                                │ 4f. Encode base64  │          │
     │                                                │ 4g. Return JSON    │          │
     │                                                └───────────┬────────┘          │
     │                                                            │                   │
     │                                                            │<──────────────────┘
     │                                                            │
     │  5. Return JSON list của violations                       │
     │<───────────────────────────────────────────────────────── │
     │                                                            │
     │ 6. Display results (plate + image)                        │
     │                                                            │
     │ 7. User clicks "Lưu vi phạm"                              │
     │────────────────────────────────────────────────────────>  │
     │     POST /api/v1/violations/save                          │
     │     {plate, image_base64}                                 │
     │                                                            │
     │                                                            │ 8. Decode base64
     │                                                            │ 9. Save image file
     │                                                            │ 10. Insert to DB
     │                                                            │
     │  11. Return saved ViolationLog                            │
     │<───────────────────────────────────────────────────────── │
     │                                                            │
     │ 12. Show success message                                  │
     │                                                            │
```

### 5.2. Sequence Diagram - Process Video

```
Browser          Controller        Service          Python Script        Models
   │                 │                │                    │                │
   │─POST /process──>│                │                    │                │
   │                 │                │                    │                │
   │                 │─processVideo()─>│                    │                │
   │                 │                │                    │                │
   │                 │                │─save temp files    │                │
   │                 │                │                    │                │
   │                 │                │─ProcessBuilder────>│                │
   │                 │                │                    │                │
   │                 │                │                    │─load models───>│
   │                 │                │                    │<───models──────│
   │                 │                │                    │                │
   │                 │                │                    │─process video──│
   │                 │                │                    │  (detect,      │
   │                 │                │                    │   track,       │
   │                 │                │                    │   OCR)         │
   │                 │                │                    │                │
   │                 │                │<───return JSON─────│                │
   │                 │                │                    │                │
   │                 │<───JSON result─│                    │                │
   │                 │                │                    │                │
   │<──JSON response─│                │                    │                │
   │                 │                │                    │                │
```

---

## 📁 VI. CẤU TRÚC THƯ MỤC DỰ ÁN

```
PT_HTTM/
│
├── README.md                          # Hướng dẫn chung
├── MIGRATION_TO_JDBC.md              # Tài liệu migration
├── BAO_CAO_DU_AN.md                  # File báo cáo này
│
├── Client/                            # Spring Boot Application
│   ├── pom.xml                        # Maven dependencies
│   ├── mvnw, mvnw.cmd                # Maven wrapper
│   │
│   ├── src/main/
│   │   ├── java/com/btl/serverapp/
│   │   │   ├── MainApplication.java          # Entry point
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── ViolationController.java  # REST API
│   │   │   │
│   │   │   ├── service/
│   │   │   │   └── ViolationService.java     # Business logic
│   │   │   │
│   │   │   ├── dao/
│   │   │   │   └── ViolationLogDAO.java      # JDBC DAO
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   └── ViolationLog.java         # POJO entity
│   │   │   │
│   │   │   └── config/                       # (empty)
│   │   │
│   │   └── resources/
│   │       ├── application.properties        # Configuration
│   │       ├── schema.sql                    # Database schema
│   │       │
│   │       └── static/                       # Frontend files
│   │           ├── index.html
│   │           ├── html/
│   │           │   ├── violation.html        # Upload page
│   │           │   ├── results.html          # Results page
│   │           │   └── history.html          # History page
│   │           │
│   │           ├── js/
│   │           │   ├── violation.app.js      # Main JS
│   │           │   └── results.app.js        # Results JS
│   │           │
│   │           ├── css/
│   │           │   └── violation.style.css   # Styling
│   │           │
│   │           └── violation_images/         # Saved evidences
│   │
│   ├── target/                        # Compiled files (Maven)
│   └── temp/                          # Temporary video files
│
└── ML/                                # Machine Learning
    └── Model_Scripts/
        ├── process.py                 # Main Python script
        │
        └── models/
            ├── yolov8_detect_xe.pt           # Vehicle detection
            ├── yolov8_detect_bienso.pt       # Plate detection
            ├── ocr_net.pt                    # OCR model
            └── README.txt
```

---

## ⚙️ VII. CẤU HÌNH VÀ TRIỂN KHAI

### 7.1. Yêu cầu hệ thống

**Phần mềm:**
- ✅ Java JDK 11 trở lên
- ✅ Maven 3.6+
- ✅ Python 3.8+
- ✅ MySQL Server 8.0+
- ✅ Git

**Thư viện Python:**
```bash
pip install ultralytics opencv-python numpy torch
```

**Hardware khuyến nghị:**
- CPU: Intel i5 hoặc tương đương
- RAM: 8GB trở lên
- GPU: NVIDIA GTX 1050+ (optional, tăng tốc xử lý)
- Storage: 5GB free space

### 7.2. File cấu hình quan trọng

#### **A. application.properties**

```properties
# Server Configuration
server.port=8080

# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/pt_httm?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=khoi21102004
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JDBC Auto-run schema.sql
spring.sql.init.mode=always
spring.sql.init.continue-on-error=false
spring.sql.init.platform=mysql

# File Upload Configuration
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB
spring.servlet.multipart.enabled=true
spring.servlet.multipart.file-size-threshold=2KB
spring.servlet.multipart.location=${java.io.tmpdir}

# Python Script Configuration
python.executable=python
python.script.path=E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/ML/Model_Scripts/process.py

# Model Paths (ABSOLUTE PATHS)
model.path.object-detect=E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/ML/Model_Scripts/models/yolov8_detect_xe.pt
model.path.plate-detect=E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/ML/Model_Scripts/models/yolov8_detect_bienso.pt
model.path.ocr=E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/ML/Model_Scripts/models/ocr_net.pt
```

**⚠️ Lưu ý:** Phải sửa đường dẫn tuyệt đối cho đúng với máy của bạn!

#### **B. process.py - Configuration**

```python
# Đường dẫn lưu ảnh bằng chứng (ABSOLUTE PATH)
STATIC_IMAGE_SAVE_PATH = "E:/tai lieu mon hoc/pt httm/vipham/PT_HTTM/Client/src/main/resources/static/violation_images/"
```

### 7.3. Hướng dẫn cài đặt và chạy

#### **Bước 1: Clone repository**
```bash
git clone https://github.com/lamant1411/PT_HTTM.git
cd PT_HTTM
```

#### **Bước 2: Cài đặt Python dependencies**
```bash
cd ML/Model_Scripts
pip install -r requirements.txt
# Hoặc cài thủ công:
pip install ultralytics opencv-python numpy torch
```

#### **Bước 3: Tạo database MySQL**
```sql
CREATE DATABASE pt_httm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### **Bước 4: Cấu hình đường dẫn**
- Sửa `Client/src/main/resources/application.properties`
  - Đường dẫn Python script
  - Đường dẫn 3 models
  - MySQL username/password
  
- Sửa `ML/Model_Scripts/process.py`
  - Đường dẫn `STATIC_IMAGE_SAVE_PATH`

#### **Bước 5: Build và chạy**
```bash
cd Client
mvn clean install
mvn spring-boot:run
```

#### **Bước 6: Truy cập ứng dụng**
```
http://localhost:8080/html/violation.html
```

### 7.4. Troubleshooting

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| `Python script not found` | Đường dẫn sai | Check `application.properties`, dùng đường dẫn tuyệt đối |
| `Cannot create PoolableConnectionFactory` | MySQL không chạy hoặc sai password | Start MySQL, check credentials |
| `Table 'violation_log' doesn't exist` | Schema chưa chạy | Check `schema.sql`, set `spring.sql.init.mode=always` |
| `Cannot delete temp file` | Windows lock file | Đã fix với `Thread.sleep(500ms)` |
| `ModuleNotFoundError: ultralytics` | Thiếu Python package | `pip install ultralytics` |
| `OutOfMemoryError` | Video quá lớn | Giảm size video hoặc tăng heap size |

---

## 🎯 VIII. TÍNH NĂNG VÀ CHỨC NĂNG

### 8.1. Chức năng đã hoàn thành ✅

| # | Chức năng | Mô tả | Status |
|---|-----------|-------|--------|
| 1 | **Upload video** | Upload file MP4, hiển thị frame đầu | ✅ Done |
| 2 | **Vẽ vạch dừng** | Click 2 điểm trên canvas, vẽ đường thẳng | ✅ Done |
| 3 | **Phát hiện xe** | YOLOv8 detect + ByteTrack tracking | ✅ Done |
| 4 | **Kiểm tra vượt vạch** | Thuật toán cross product | ✅ Done |
| 5 | **Detect biển số** | YOLOv8 phát hiện vùng biển số | ✅ Done |
| 6 | **OCR biển số** | Đọc ký tự trên biển số | ✅ Done |
| 7 | **Encode base64** | Convert ảnh → base64 gửi frontend | ✅ Done |
| 8 | **Hiển thị kết quả** | List violations với ảnh + biển số | ✅ Done |
| 9 | **Lưu vào database** | Insert MySQL với JDBC | ✅ Done |
| 10 | **Lưu ảnh bằng chứng** | Decode base64 → save file | ✅ Done |
| 11 | **Xem lịch sử** | Query database, hiển thị list | ✅ Done |
| 12 | **REST API** | 2 endpoints (process, save) | ✅ Done |

### 8.2. Demo Flow

**Input:**
- Video giao thông (MP4, độ dài 30s-5 phút)
- 2 điểm tọa độ vạch dừng (x1,y1), (x2,y2)

**Processing:**
- Phân tích ~900-9000 frames (tùy FPS)
- Detect + track xe
- Check từng xe có vượt vạch không
- OCR biển số xe vi phạm

**Output:**
- JSON array chứa danh sách vi phạm
- Mỗi violation có: frame, time, plate, image_base64, confidence
- Lưu vào database + file system khi user confirm

**Thời gian xử lý:**
- Video 1 phút @ 30fps: ~30-60 giây (tùy hardware)
- Video 5 phút: ~3-5 phút

### 8.3. Ví dụ kết quả

**JSON Output:**
```json
[
  {
    "frame": 150,
    "video_time": "00:05",
    "license_plate": "29A-12345",
    "evidence_image_base64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
    "confidence": 0.92
  },
  {
    "frame": 450,
    "video_time": "00:15",
    "license_plate": "30B-67890",
    "evidence_image_base64": "data:image/jpeg;base64,iVBORw0KGgo...",
    "confidence": 0.87
  }
]
```

**Database Record:**
```
id: 1
plate_num: 29A-12345
timestamp: 2025-11-03 10:30:15
evidence_url: /violation_images/violation_abc123.jpg
log_details: NULL
```

---

## 📊 IX. ĐÁNH GIÁ VÀ KẾT QUẢ

### 9.1. Ưu điểm ✅

1. **Kiến trúc rõ ràng**
   - Phân tách frontend/backend/AI
   - RESTful API chuẩn
   - JDBC thuần, không phụ thuộc JPA

2. **Tính năng hoàn chỉnh**
   - Upload video, custom stop line
   - AI detection tự động
   - Lưu trữ persistent

3. **Công nghệ hiện đại**
   - YOLOv8 (state-of-the-art object detection)
   - ByteTrack (robust tracking)
   - Spring Boot (industry standard)

4. **UX tốt**
   - Canvas drawing trực quan
   - Loading indicator
   - Results preview trước khi save

5. **Code quality**
   - Comments đầy đủ
   - Error handling
   - Resource cleanup (try-with-resources)

### 9.2. Nhược điểm / Hạn chế ⚠️

1. **Performance**
   - Xử lý video chậm với file lớn
   - Không có queue/async processing
   - Load 3 models mỗi lần chạy (có thể cache)

2. **Accuracy**
   - OCR phụ thuộc chất lượng video
   - Góc quay camera ảnh hưởng detection
   - Lighting conditions quan trọng

3. **Scalability**
   - Chỉ xử lý 1 video tại 1 thời điểm
   - Không có distributed processing
   - Temp files có thể tích lũy nếu lỗi cleanup

4. **Security**
   - Không có authentication/authorization
   - Upload file không validate format kỹ
   - Đường dẫn hard-coded (không flexible)

5. **Features thiếu**
   - Không có export report (PDF/Excel)
   - Không có statistics dashboard
   - Không có notification system

### 9.3. Đề xuất cải tiến 🚀

#### **Phase 2 - Nâng cao:**

1. **Performance Optimization**
   - Cache AI models (load 1 lần duy nhất)
   - Async processing với queue (Redis/RabbitMQ)
   - Multi-threading cho frame processing
   - GPU acceleration

2. **Accuracy Improvement**
   - Fine-tune models với dataset Việt Nam
   - Ensemble OCR (kết hợp nhiều models)
   - Post-processing: filter, normalize plate text
   - Confidence threshold tuning

3. **Scalability**
   - Microservices architecture
   - Separate AI service (FastAPI/Flask)
   - Load balancer
   - Cloud deployment (AWS/Azure)

4. **Security**
   - Spring Security với JWT
   - Role-based access control (Admin/User)
   - File upload validation (size, type, virus scan)
   - HTTPS/SSL

5. **New Features**
   - Real-time video streaming
   - Dashboard với charts (violations by time, location)
   - Export reports (PDF, Excel)
   - Email/SMS notification
   - Mobile app (React Native)
   - Multiple camera support

6. **DevOps**
   - Docker containerization
   - CI/CD pipeline (Jenkins/GitLab CI)
   - Automated testing (Unit, Integration)
   - Monitoring (Prometheus, Grafana)
   - Logging (ELK stack)

---

## 📚 X. TÀI LIỆU THAM KHẢO

### 10.1. Technologies

1. **Spring Boot**
   - https://spring.io/projects/spring-boot
   - Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/

2. **YOLOv8**
   - https://github.com/ultralytics/ultralytics
   - Paper: "YOLOv8: You Only Look Once version 8"

3. **OpenCV**
   - https://opencv.org/
   - Python docs: https://docs.opencv.org/4.x/

4. **ByteTrack**
   - Paper: "ByteTrack: Multi-Object Tracking by Associating Every Detection Box"
   - https://github.com/ifzhang/ByteTrack

5. **MySQL**
   - https://dev.mysql.com/doc/

### 10.2. Concepts

- Object Detection: https://paperswithcode.com/task/object-detection
- Object Tracking: https://paperswithcode.com/task/multi-object-tracking
- OCR: https://en.wikipedia.org/wiki/Optical_character_recognition
- RESTful API: https://restfulapi.net/

### 10.3. Tools

- Maven: https://maven.apache.org/
- Git: https://git-scm.com/
- Postman: https://www.postman.com/ (API testing)
- MySQL Workbench: https://www.mysql.com/products/workbench/

---

## 👥 XI. THÔNG TIN DỰ ÁN

### 11.1. Repository
- **GitHub:** https://github.com/lamant1411/PT_HTTM
- **Branch:** main
- **Last Updated:** November 3, 2025

### 11.2. Tech Stack Summary

```
Backend:    Java 11 + Spring Boot 2.7.12 + JDBC + MySQL 8.0
Frontend:   HTML5 + CSS3 + JavaScript ES6+ + Canvas API
AI/ML:      Python 3.8+ + YOLOv8 + OpenCV + ByteTrack
Build:      Maven 3.6+
Server:     Embedded Tomcat (Spring Boot)
Database:   MySQL 8.0+ (InnoDB)
```

### 11.3. Project Stats

```
Backend:
- Java classes: 5
- REST endpoints: 2
- Database tables: 1
- Lines of code: ~800

Frontend:
- HTML pages: 4
- JavaScript files: 2
- CSS files: 1
- Lines of code: ~600

AI/ML:
- Python scripts: 1
- Models: 3 (YOLOv8 x2, OCR x1)
- Lines of code: ~400

Total LoC: ~1800+
```

---

## ✅ XII. KẾT LUẬN

### 12.1. Tổng kết

Đồ án **"Hệ thống Phát hiện Vi phạm Giao thông Tự động"** đã hoàn thành đầy đủ các mục tiêu đề ra:

✅ **Xây dựng thành công** hệ thống web application hoàn chỉnh  
✅ **Tích hợp AI/ML** để phát hiện vi phạm tự động  
✅ **Triển khai database** MySQL với JDBC thuần  
✅ **Giao diện thân thiện** với canvas drawing  
✅ **API RESTful** chuẩn  
✅ **Code quality tốt** với comments đầy đủ  

### 12.2. Kỹ năng đạt được

Qua quá trình làm đồ án, sinh viên đã nắm vững:

1. **Backend Development**
   - Spring Boot framework
   - REST API design
   - JDBC programming
   - File handling & multipart upload

2. **Frontend Development**
   - HTML5 Canvas API
   - JavaScript async/await
   - Fetch API
   - Base64 encoding/decoding

3. **AI/ML Integration**
   - YOLOv8 object detection
   - Object tracking (ByteTrack)
   - OCR (Optical Character Recognition)
   - Python-Java integration

4. **Database**
   - MySQL schema design
   - JDBC operations (CRUD)
   - PreparedStatement (SQL Injection prevention)

5. **DevOps**
   - Maven build tool
   - Git version control
   - Configuration management

### 12.3. Ứng dụng thực tế

Hệ thống có thể áp dụng cho:

- 🚦 Cơ quan quản lý giao thông
- 🏛️ Công an giao thông
- 🏢 Công ty an ninh, camera giám sát
- 🎓 Đề tài nghiên cứu khoa học
- 💼 Startup công nghệ giao thông thông minh

### 12.4. Đóng góp

Đồ án đóng góp vào việc:

- ✅ Tự động hóa công tác giám sát giao thông
- ✅ Giảm tải công việc cho cán bộ CSGT
- ✅ Tăng tính khách quan trong xử phạt
- ✅ Lưu trữ bằng chứng vi phạm có hệ thống
- ✅ Ứng dụng AI vào bài toán thực tế

---

## 📞 XIII. LIÊN HỆ VÀ HỖ TRỢ

Nếu có thắc mắc hoặc cần hỗ trợ:

- **GitHub Issues:** https://github.com/lamant1411/PT_HTTM/issues
- **Documentation:** Xem các file `.md` trong repository
- **Email:** [Thêm email nếu có]

---

**🎓 Cảm ơn thầy đã theo dõi báo cáo!**

---

*Generated: November 3, 2025*  
*Version: 1.0*  
*Format: Markdown*
