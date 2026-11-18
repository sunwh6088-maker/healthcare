package com.healthcare.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用入口：启动 Spring Boot，Flyway 自动执行数据库迁移。
 */
@SpringBootApplication
@EnableScheduling
public class HealthcareApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthcareApiApplication.class, args);
    }
}
