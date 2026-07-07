package com.msc.springai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        System.out.println("[JacksonConfig] Create ObjectMapper bean.");
        return new ObjectMapper();
    }
}