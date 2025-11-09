# ServerML - Problem Management API

Server quản lý bài toán (Problem) cho hệ thống phát hiện vi phạm giao thông.

## Cấu trúc Project

```
ServerML/
├── src/
│   ├── main/
│   │   ├── java/com/btl/serverml/
│   │   │   ├── MainApplication.java
│   │   │   ├── config/
│   │   │   │   └── DatabaseConfig.java
│   │   │   ├── controller/
│   │   │   │   └── ProblemController.java
│   │   │   ├── service/
│   │   │   │   └── ProblemService.java
│   │   │   ├── dao/
│   │   │   │   └── ProblemDAO.java
│   │   │   └── entity/
│   │   │       └── Problem.java
│   │   └── resources/
│   │       └── application.properties
├── pom.xml
└── database.sql
```

## Cài đặt

### 1. Tạo database

```sql
-- Chạy file database.sql trong MySQL
mysql -u root -p violation_db < database.sql
```

### 2. Cấu hình database

Chỉnh sửa file `application.properties` nếu cần:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/violation_db
spring.datasource.username=root
spring.datasource.password=
```

### 3. Build và chạy

```bash
# Build project
mvn clean install

# Chạy server
mvn spring-boot:run
```

Server sẽ chạy tại: `http://localhost:8081`

## API Endpoints

### 1. Lấy tất cả problems
```
GET /api/problems
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Phát hiện xe vượt đèn đỏ",
    "description": "Hệ thống phát hiện và ghi nhận các phương tiện vi phạm vượt đèn đỏ tại giao lộ"
  }
]
```

### 2. Lấy problem theo ID
```
GET /api/problems/{id}
```

**Response:**
```json
{
  "id": 1,
  "name": "Phát hiện xe vượt đèn đỏ",
  "description": "Hệ thống phát hiện và ghi nhận các phương tiện vi phạm vượt đèn đỏ tại giao lộ"
}
```

### 3. Tạo mới problem
```
POST /api/problems
Content-Type: application/json

{
  "name": "Tên bài toán",
  "description": "Mô tả bài toán"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Tạo bài toán thành công"
}
```

### 4. Cập nhật problem
```
PUT /api/problems/{id}
Content-Type: application/json

{
  "name": "Tên bài toán mới",
  "description": "Mô tả mới"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Cập nhật bài toán thành công"
}
```

### 5. Xóa problem
```
DELETE /api/problems/{id}
```

**Response:**
```json
{
  "success": true,
  "message": "Xóa bài toán thành công"
}
```

## Kiến trúc

- **Entity**: `Problem` - POJO chứa dữ liệu
- **DAO**: `ProblemDAO` - Truy vấn database bằng JdbcTemplate
- **Service**: `ProblemService` - Business logic, validation
- **Controller**: `ProblemController` - REST API endpoints
- **Config**: `DatabaseConfig` - Cấu hình DataSource và JdbcTemplate

## Technologies

- Spring Boot 2.7.12
- Spring Web
- Spring JDBC
- MySQL 8.0
- Maven
