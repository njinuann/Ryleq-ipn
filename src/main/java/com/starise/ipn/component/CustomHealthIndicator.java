//package com.starise.ipn.component;
//
//import org.springframework.boot.actuate.health.Health;
//import org.springframework.boot.actuate.health.HealthIndicator;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CustomHealthIndicator implements HealthIndicator {
//
//    @Override
//    public Health health() {
//        // Your custom logic (e.g., check database connectivity, external API, etc.)
//        boolean serviceUp = checkServiceHealth();
//        if (serviceUp) {
//            return Health.up().withDetail("Service", "Available").build();
//        } else {
//            return Health.down().withDetail("Service", "Unavailable").build();
//        }
//    }
//
//    private boolean checkServiceHealth() {
//        // Simulate service check (replace with real logic)
//        return true; // or false if the service is down
//    }
//}
