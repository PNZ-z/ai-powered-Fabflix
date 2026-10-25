package com.fabflix.boot.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Drives the probe against a loopback stub on 127.0.0.1 with a random port, so no Ollama
 * instance, LAN endpoint or fixed port is involved and CI stays self-contained.
 */
class HttpOllamaReachabilityProbeTest {
    private static final List<String> MODEL_LOADING_OR_INVENTORY_PATHS = List.of(
            "/api/tags", "/api/ps", "/api/chat", "/api/generate", "/api/embed");

    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();
    private final List<String> requestedMethods = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @AfterEach
    void stopsTheStub() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void asksOnlyTheVersionEndpointAndReportsReachable() throws Exception {
        startStub(exchange -> respond(exchange, 200, "{\"version\":\"0.0.0\"}"));

        boolean reachable = new HttpOllamaReachabilityProbe(baseUrl() + "/").isReachable(Duration.ofSeconds(2));

        assertTrue(reachable);
        assertEquals(1, requestedPaths.size(), () -> "probe made requests: " + requestedPaths);
        assertEquals("/api/version", requestedPaths.get(0));
        assertEquals(List.of("GET"), requestedMethods, () -> "probe used methods: " + requestedMethods);
        for (String forbidden : MODEL_LOADING_OR_INVENTORY_PATHS) {
            assertFalse(requestedPaths.contains(forbidden), () -> "probe requested " + forbidden);
        }
        assertTrue(requestedPaths.stream().noneMatch(path -> path.startsWith("/v1/")),
                () -> "probe requested an OpenAI-compatible path: " + requestedPaths);
    }

    @Test
    void reportsUnreachableForANonSuccessStatus() throws Exception {
        startStub(exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));

        assertFalse(new HttpOllamaReachabilityProbe(baseUrl()).isReachable(Duration.ofSeconds(2)));
    }

    @Test
    void appliesOneWholeProbeDeadline() throws Exception {
        startStub(exchange -> {
            Thread.sleep(600);
            respond(exchange, 200, "{\"version\":\"0.0.0\"}");
        });

        long startedNanos = System.nanoTime();
        boolean reachable = new HttpOllamaReachabilityProbe(baseUrl()).isReachable(Duration.ofMillis(150));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

        assertFalse(reachable);
        assertTrue(elapsedMillis >= 100, () -> "probe gave up after only " + elapsedMillis + " ms, before its 150 ms deadline");
        assertTrue(elapsedMillis < 1_000, () -> "probe took " + elapsedMillis + " ms, far beyond its 150 ms deadline");
    }

    @Test
    void reportsUnreachableWhenThePortIsClosed() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        HttpOllamaReachabilityProbe probe = new HttpOllamaReachabilityProbe("http://127.0.0.1:" + closedPort);

        assertFalse(probe.isReachable(Duration.ofSeconds(2)));
    }

    @Test
    void reportsUnreachableForAMalformedOrBlankBaseUrl() {
        assertFalse(new HttpOllamaReachabilityProbe("ht!tp://example").isReachable(Duration.ofMillis(200)));
        assertFalse(new HttpOllamaReachabilityProbe("").isReachable(Duration.ofMillis(200)));
        assertFalse(new HttpOllamaReachabilityProbe(null).isReachable(Duration.ofMillis(200)));
    }

    @Test
    void restoresTheInterruptFlagWhenInterrupted() throws Exception {
        startStub(exchange -> {
            Thread.sleep(600);
            respond(exchange, 200, "{\"version\":\"0.0.0\"}");
        });
        Thread.currentThread().interrupt();
        try {
            assertFalse(new HttpOllamaReachabilityProbe(baseUrl()).isReachable(Duration.ofSeconds(2)));
            assertTrue(Thread.currentThread().isInterrupted(), "the interrupt flag must survive the probe");
        } finally {
            Thread.interrupted();
        }
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void startStub(StubHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
            requestedMethods.add(exchange.getRequestMethod());
            try {
                handler.handle(exchange);
            } catch (IOException | InterruptedException exception) {
                // The probe cancelled the request or the stub is shutting down; nothing to report.
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(payload);
        }
    }

    @FunctionalInterface
    private interface StubHandler {
        void handle(HttpExchange exchange) throws IOException, InterruptedException;
    }
}
