package com.fabflix.boot.ai;

import java.time.Duration;

/**
 * Answers whether the configured Ollama HTTP endpoint responds at all. Probes must
 * stay on a read-only, model-free endpoint: loading a model to "prove" reachability
 * would cost seconds and can evict the other model from VRAM.
 */
public interface OllamaReachabilityProbe {
    /**
     * @param timeout the whole-probe deadline (one overall budget, never per phase)
     * @return {@code true} only when the probe endpoint answers 2xx in time; never throws
     */
    boolean isReachable(Duration timeout);
}
