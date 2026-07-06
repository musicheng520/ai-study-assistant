package com.msc.springai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.msc.springai.mapper")
public class StudyAssistentApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyAssistentApplication.class, args);
    }
}