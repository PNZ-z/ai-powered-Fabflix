package com.fabflix.boot.star;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StarDetailController {
    private static final Logger logger = LoggerFactory.getLogger(StarDetailController.class);

    private final StarDetailService starDetailService;

    public StarDetailController(StarDetailService starDetailService) {
        this.starDetailService = starDetailService;
    }

    @GetMapping("/api/boot/single-star")
    public ResponseEntity<StarDetail> getStar(
            @RequestParam(value = "id", required = false) String starId) {
        StarDetail star = starDetailService.getStar(starId);
        logger.info("Star detail request completed: starId={}", star.getStarId());
        return ResponseEntity.ok(star);
    }
}
