package com.fabflix.boot.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Probes {@code GET {baseUrl}/api/version} with the JDK HTTP client. Nothing else is
 * requested: {@code /api/chat}, {@code /api/generate}, {@code /api/embed},
 * {@code /api/tags}, {@code /api/ps} or any {@code /v1/**} path would either load a
 * model or expose inventory that this endpoint must not reveal.
 */
@Component
public class HttpOllamaReachabilityProbe implements OllamaReachabilityProbe {
    private static final Logger LOG = LoggerFactory.getLogger(HttpOllamaReachabilityProbe.class);
    private static final String VERSION_PATH = "/api/version";

    private final HttpClient httpClient;
    private final String baseUrl;

    public HttpOllamaReachabilityProbe(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        // No separate connect timeout on purpose. The request timeout and the overall
        // deadline in isReachable are both the caller's single budget, so a slow connect
        // cannot stack a second timeout on top of it; the request timeout additionally
        // terminates the underlying exchange once that budget expires instead of leaving
        // the connection polling in the background.
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public boolean isReachable(Duration timeout) {
        long startedNanos = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(versionUri(baseUrl)).GET().timeout(timeout).build();
            HttpResponse<Void> response = httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .get();
            int statusCode = response.statusCode();
            boolean reachable = statusCode >= 200 && statusCode < 300;
            LOG.debug("Ollama reachability probe answered statusCode={}, reachable={}, elapsedMs={}",
                    statusCode, reachable, elapsedMillis(startedNanos));
            return reachable;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.warn("Ollama reachability probe interrupted after {} ms", elapsedMillis(startedNanos));
            return false;
        } catch (Exception exception) {
            // Escaping here would turn a health probe into a 5xx, and the exception message
            // can carry the endpoint, so only the failure type is logged.
            LOG.warn("Ollama reachability probe failed ({}); treating the endpoint as unreachable after {} ms",
                    rootCauseName(exception), elapsedMillis(startedNanos));
            return false;
        }
    }

    private static URI versionUri(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Ollama base URL is blank");
        }
        String withoutTrailingSlashes = trimmed;
        while (withoutTrailingSlashes.endsWith("/")) {
            withoutTrailingSlashes = withoutTrailingSlashes.substring(0, withoutTrailingSlashes.length() - 1);
        }
        return URI.create(withoutTrailingSlashes + VERSION_PATH);
    }

    private static String rootCauseName(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
