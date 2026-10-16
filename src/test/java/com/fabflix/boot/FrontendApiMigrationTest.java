package com.fabflix.boot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards the retirement of the legacy HTML/JavaScript frontend. The React
 * application under {@code frontend/} is the only supported frontend and is
 * served by Spring Boot, so {@code src/main/webapp} must stay gone.
 */
class FrontendApiMigrationTest {
    private static final Path WEBAPP = Path.of("src", "main", "webapp");

    @Test
    void legacyWebappDirectoryIsRemoved() {
        assertFalse(Files.exists(WEBAPP),
                () -> WEBAPP + " must stay removed; frontend/ is the only supported frontend");
    }

    @Test
    void noLegacyFrontendFilesRemain() throws IOException {
        if (!Files.exists(WEBAPP)) return;

        try (Stream<Path> files = Files.walk(WEBAPP)) {
            List<Path> legacyFiles = files.filter(Files::isRegularFile)
                    .filter(FrontendApiMigrationTest::isLegacyFrontendFile)
                    .collect(Collectors.toList());
            assertTrue(legacyFiles.isEmpty(),
                    () -> "Retired legacy frontend files reappeared: " + legacyFiles);
        }
    }

    private static boolean isLegacyFrontendFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".html") || name.endsWith(".js");
    }
}
