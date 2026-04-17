package com.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 核心注解：标识这是一个 Spring Boot 应用
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}