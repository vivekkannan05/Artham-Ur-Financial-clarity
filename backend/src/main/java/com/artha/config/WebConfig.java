package com.artha.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration — CORS settings.
 *
 * SPEC REF: NFR-8.2 — CORS allowed origins from config.
 *
 * WHY CORS?
 *   The React frontend (localhost:3000) makes API calls to the backend (localhost:8080).
 *   Browsers block cross-origin requests by default (security feature).
 *   CORS headers tell the browser: "It's OK, this origin is allowed."
 *
 *   Without this config, the frontend would get:
 *   "Access to fetch at 'http://localhost:8080/...' from origin 'http://localhost:3000'
 *    has been blocked by CORS policy."
 */
@Configuration
public class WebConfig {

    private final ArthaProperties arthaProperties;

    public WebConfig(ArthaProperties arthaProperties) {
        this.arthaProperties = arthaProperties;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = arthaProperties.getCors().getAllowedOrigins().split(",");
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                        .allowedHeaders("*")
                        .maxAge(3600); // Cache preflight response for 1 hour
            }
        };
    }
}
