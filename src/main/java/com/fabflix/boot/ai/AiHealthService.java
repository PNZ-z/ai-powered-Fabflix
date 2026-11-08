package com.fabflix.boot.ai;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Assembles the AI health answer. When {@code fabflix.ai.enabled} is false the probe is
 * never called, so a disabled deployment makes no outbound connection at all.
 *
 * <p>Only {@code true} (case-insensitive) enables AI; an empty value or anything else is
 * treated as {@code false}, so a misconfigured switch degrades to "disabled" instead of
 * failing the request.
 */
@Service
public class AiHealthService {
    private static final Logger LOG = LoggerFactory.getLogger(AiHealthService.class);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);
    private static final String ENABLED_VALUE = "true";
    private static final String STATUS_DISABLED = "disabled";
    private static final String STATUS_OK = "ok";
    private static final String STATUS_UNREACHABLE = "unreachable";

    private final OllamaReachabilityProbe reachabilityProbe;
    private final boolean enabled;
    private final String chatModel;
    private final String embeddingModel;

    public AiHealthService(
            OllamaReachabilityProbe reachabilityProbe,
            @Value("${fabflix.ai.enabled}") String enabled,
            @Value("${spring.ai.ollama.chat.options.model}") String chatModel,
            @Value("${spring.ai.ollama.embedding.options.model}") String embeddingModel) {
        this.reachabilityProbe = reachabilityProbe;
        this.enabled = ENABLED_VALUE.equalsIgnoreCase(enabled == null ? "" : enabled.trim());
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    public AiHealthResponse health() {
        if (!enabled) {
            return new AiHealthResponse(false, chatModel, embeddingModel, false, STATUS_DISABLED);
        }

        boolean reachable;
        try {
            reachable = reachabilityProbe.isReachable(PROBE_TIMEOUT);
        } catch (RuntimeException exception) {
            // A probe that throws must not surface as a 5xx: report unreachable instead.
            LOG.warn("AI reachability probe raised {}; reporting the endpoint as unreachable",
                    exception.getClass().getSimpleName());
            reachable = false;
        }
        return new AiHealthResponse(
                true, chatModel, embeddingModel, reachable, reachable ? STATUS_OK : STATUS_UNREACHABLE);
    }
}
