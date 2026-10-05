package com.fabflix.boot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FrontendApiMigrationTest {
    private static final Path WEBAPP = Path.of("src", "main", "webapp");

    @Test
    void frontendNoLongerCallsLegacyServletApiPaths() throws IOException {
        try (Stream<Path> files = Files.walk(WEBAPP)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isFrontendSource)
                    .forEach(path -> assertNoLegacyApiPath(path, read(path)));
        }
    }

    @Test
    void criticalPagesUseTheirSpringBootEndpoints() {
        assertContains("login.js", "api/boot/login");
        assertContains("main.js", "api/boot/main", "api/boot/autocomplete");
        assertContains("movieList.js", "api/boot/movie-list", "api/boot/full-text-search", "api/boot/cart");
        assertContains("single-movie.js", "api/boot/single-movie", "api/boot/cart");
        assertContains("single-star.js", "api/boot/single-star");
        assertContains("shoppingCart.js", "api/boot/cart");
        assertContains("payment.js", "api/boot/cart", "api/boot/payment");
        assertContains("_dashboard.js", "api/boot/employee-login");
        assertContains("dashboard.js", "api/boot/dashboard");
    }

    private boolean isFrontendSource(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".js") || name.endsWith(".html");
    }

    private void assertNoLegacyApiPath(Path path, String content) {
        assertFalse(content.matches("(?s).*[\\\"'=]api/(?!boot/).*"),
                () -> path + " still contains a legacy API URL");
    }

    private void assertContains(String file, String... endpoints) {
        String content = read(WEBAPP.resolve(file));
        for (String endpoint : endpoints) {
            assertTrue(content.contains(endpoint), () -> file + " must reference " + endpoint);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
