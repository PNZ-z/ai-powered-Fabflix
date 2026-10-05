package com.fabflix.boot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SpringBootOnlyArchitectureTest {
    private static final Path MAIN_JAVA = Path.of("src", "main", "java");

    @Test
    void onlineApplicationDoesNotReintroduceLegacyServlets() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::assertNotLegacyServlet);
        }
    }

    @Test
    void buildProducesAnExecutableSpringBootJarWithoutJakartaServletDependency() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<packaging>jar</packaging>"));
        assertTrue(pom.contains("<goal>repackage</goal>"));
        assertFalse(pom.contains("jakarta.servlet-api"));
    }

    private void assertNotLegacyServlet(Path path) {
        String source = read(path);
        assertFalse(source.contains("jakarta.servlet"), () -> path + " imports the retired Jakarta Servlet API");
        assertFalse(source.contains("@WebServlet"), () -> path + " declares a retired servlet endpoint");
        assertFalse(source.contains("extends HttpServlet"), () -> path + " implements a retired servlet endpoint");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
