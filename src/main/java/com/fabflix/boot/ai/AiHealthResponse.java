package com.fabflix.boot.ai;

/**
 * Body of {@code GET /api/boot/ai/health}.
 *
 * <p>{@code status} is a health-endpoint-family field (the sibling
 * {@code /api/boot/health} answers {@code status: "UP"}), not a business
 * {@code success}/{@code fail} result: {@code unreachable} is still answered with
 * HTTP 200. {@code reachable = true} only proves that {@code GET /api/version}
 * answered 2xx; it says nothing about the configured models being installed or
 * loaded. The base URL is deliberately not echoed back.
 *
 * <p>When {@code enabled = false} the probe is never called, so {@code reachable = false}
 * means "not probed" rather than "probed and found unreachable"; {@code status = disabled}
 * is what distinguishes the two.
 */
public record AiHealthResponse(
        boolean enabled,
        String chatModel,
        String embeddingModel,
        boolean reachable,
        String status) {
}
