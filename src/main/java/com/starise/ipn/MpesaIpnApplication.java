package com.starise.ipn;

//import com.starise.ipn.service.IpnService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@SpringBootApplication
//@EntityScan(basePackages = "com.starise.ipn.entity")
//@EnableJpaRepositories(basePackages = "com.starise.ipn.repository")
//@ComponentScan(basePackages = {"com.starise.ipn"})
//@EnableJpaRepositories(basePackages = "com.starise.ipn.repository")
public class MpesaIpnApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpesaIpnApplication.class, args);
    }

    private static final Logger logger = LoggerFactory.getLogger(MpesaIpnApplication.class);

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(10000); // Adjust this as needed
        loggingFilter.setIncludeHeaders(true); // You can disable this if not needed
        return loggingFilter;
    }

    @Component
    public static class LoggingInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
            //	String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            logger.info("Request URL: {}", request.getRequestURL());
            logger.info("Request Method: {}", request.getMethod());
            logger.info("Request Headers: {}", request.getHeaderNames());
            logger.info("Request Body: {}", request.getRemoteUser());
            // Log other details as needed
            return true;
        }
    }


    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Autowired
        private LoggingInterceptor loggingInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(loggingInterceptor);
        }
    }

    @ControllerAdvice
    public static class GlobalExceptionHandler {
        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handleException(Exception ex) {
            logger.error("Exception occurred: ", ex);
            return new ResponseEntity<>("An error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


