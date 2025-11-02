package com.btl.serverapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application file for Client Application (port 8080).
 * The @SpringBootApplication annotation will automatically configure
 * and scan components in sub-packages (controller, service, etc.).
 */
@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        // Start Spring Boot application
        SpringApplication.run(MainApplication.class, args);
        
        System.out.println("--- CLIENT APPLICATION (PORT 8080) IS RUNNING ---");
        System.out.println("Access UI: http://localhost:8080/html/violation.html");
    }
}