//package com.starise.ipn.Util;
//
//import org.jetbrains.annotations.NotNull;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig {
//
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(@NotNull CorsRegistry registry) {
//                registry.addMapping("/**") // Allow all paths
//                        .allowedOrigins("*") // Allow specific origins
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed methods
//                        .allowedHeaders("*") // Allow all headers
//                        .allowCredentials(true); // Allow credentials (cookies, etc.)
//            }
//        };
//    }
//}
