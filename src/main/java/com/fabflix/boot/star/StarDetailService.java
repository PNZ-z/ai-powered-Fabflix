package com.fabflix.boot.star;

import org.springframework.stereotype.Service;

@Service
public class StarDetailService {
    private final StarDetailRepository starDetailRepository;

    public StarDetailService(StarDetailRepository starDetailRepository) {
        this.starDetailRepository = starDetailRepository;
    }

    public StarDetail getStar(String starId) {
        String normalizedStarId = starId == null ? null : starId.trim();
        if (normalizedStarId == null || normalizedStarId.isEmpty()) {
            throw new IllegalArgumentException("Star id is required.");
        }

        return starDetailRepository.findById(normalizedStarId)
                .orElseThrow(() -> new StarNotFoundException(normalizedStarId));
    }
}
