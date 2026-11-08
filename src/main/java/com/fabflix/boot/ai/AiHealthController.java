package com.fabflix.boot.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Under {@code /api/boot/} and not in {@code CustomerAuthenticationFilter}'s allow-list,
 * so an anonymous request is answered 401 by the existing filter without any filter change.
 */
@RestController
public class AiHealthController {
    private final AiHealthService aiHealthService;

    public AiHealthController(AiHealthService aiHealthService) {
        this.aiHealthService = aiHealthService;
    }

    @GetMapping("/api/boot/ai/health")
    public AiHealthResponse health() {
        return aiHealthService.health();
    }
}
