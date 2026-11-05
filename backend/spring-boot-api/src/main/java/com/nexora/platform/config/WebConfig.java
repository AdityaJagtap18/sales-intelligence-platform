package com.nexora.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 4200 is the standard `ng serve` port; 4201 is what the CLI falls back to
    // when 4200 is already taken by another local project. CORS_ALLOWED_ORIGINS
    // adds the deployed origin(s) in production without touching this source.
    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:4201}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }
}
