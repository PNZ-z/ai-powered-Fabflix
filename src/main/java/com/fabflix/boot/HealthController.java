package com.fabflix.boot;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/boot/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "application", "fabflix-spring-boot"
        );
    }
}
