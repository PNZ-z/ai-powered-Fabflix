package com.fabflix.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SpringBootOnlyArchitectureTest {
    private static final Path MAIN_JAVA = Path.of("src", "main", "java");

    /**
     * The online application is a Spring Boot web application: Spring MVC controllers,
     * services and filters legitimately receive the container's Jakarta Servlet request,
     * response and session types. The API is therefore limited to the files below, and
     * set equality (not containment) means a new opt-in file must be added here
     * deliberately instead of widening the allow-list by accident.
     */
    private static final Set<String> JAKARTA_SERVLET_ALLOW_LIST = Set.of(
            "com/fabflix/boot/admin/EmployeeAuthenticationFilter.java",
            "com/fabflix/boot/admin/EmployeeController.java",
            "com/fabflix/boot/auth/CustomerAuthController.java",
            "com/fabflix/boot/auth/CustomerAuthenticationFilter.java",
            "com/fabflix/boot/commerce/CartController.java",
            "com/fabflix/boot/commerce/CartService.java",
            "com/fabflix/boot/commerce/CheckoutController.java",
            "com/fabflix/boot/commerce/CheckoutTokenService.java",
            "com/fabflix/boot/commerce/PaymentController.java",
            "com/fabflix/boot/search/SearchController.java",
            "com/fabflix/boot/session/SessionController.java");

    @Test
    void onlineApplicationConfinesJakartaServletApiToTheReviewedAllowList() throws IOException {
        assertEquals(
                new TreeSet<>(JAKARTA_SERVLET_ALLOW_LIST),
                filesContaining("jakarta.servlet"),
                "src/main/java files using the Jakarta Servlet API differ from the reviewed allow-list");

        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::assertNoLegacyServletEndpoint);
        }
    }

    @Test
    void buildProducesAnExecutableSpringBootJarWithoutJakartaServletDependency() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<packaging>jar</packaging>"));
        assertTrue(pom.contains("<goal>repackage</goal>"));
        // Expected to stay absent: spring-boot-starter-web provides the Servlet API
        // transitively, so the pom must not declare it directly.
        assertFalse(pom.contains("jakarta.servlet-api"));
        assertFalse(pom.contains("javax.servlet-api"));
    }

    private void assertNoLegacyServletEndpoint(Path path) {
        String source = read(path);
        assertFalse(source.contains("javax.servlet"), () -> path + " imports the retired javax.servlet API");
        assertFalse(source.contains("jakarta.servlet.annotation.WebServlet"), () -> path + " imports a retired servlet annotation");
        assertFalse(source.contains("@WebServlet"), () -> path + " declares a retired servlet endpoint");
        assertFalse(source.contains("extends HttpServlet"), () -> path + " implements a retired servlet endpoint");
    }

    private Set<String> filesContaining(String token) throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains(token))
                    .map(SpringBootOnlyArchitectureTest::relativePath)
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static String relativePath(Path path) {
        return MAIN_JAVA.relativize(path).toString().replace(File.separatorChar, '/');
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
