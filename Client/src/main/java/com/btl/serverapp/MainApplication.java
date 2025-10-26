package com.btl.serverapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Đây là file khởi động chính cho Client Application (cổng 8080).
 * Annotation @SpringBootApplication sẽ tự động cấu hình
 * và quét các component trong các package con (như controller, service, ...).
 */
@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        // Khởi chạy ứng dụng Spring Boot
        SpringApplication.run(MainApplication.class, args);
        
        System.out.println("--- CLIENT APPLICATION (PORT 8080) IS RUNNING ---");
        System.out.println("Truy cập giao diện: http://localhost:8080/html/violation.html");
    }
}