package com.btl.serverml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Đây là file khởi động chính cho ML Admin Application (cổng 8081).
 * Nó sẽ quét các component (controller, entity,...) trong com.btl.serverml.
 */
@SpringBootApplication
public class MlAdminApplication {

    public static void main(String[] args) {
        // Khởi chạy ứng dụng Spring Boot
        SpringApplication.run(MlAdminApplication.class, args);

        System.out.println("--- ML ADMIN APPLICATION (PORT 8081) IS RUNNING ---");
        System.out.println("Truy cập CSDL H2 (nếu dùng): http://localhost:8081/h2-console");
        System.out.println("Truy cập trang quản lý: http://localhost:8081/");
    }
}