package com.fabflix.boot.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

class SpaFallbackTest {

    // Exposes the registry's protected handler mapping so the wiring can be
    // asserted without starting a Spring context (no @SpringBootTest anywhere).
    private static final class InspectableRegistry extends ResourceHandlerRegistry {
        InspectableRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
            super(applicationContext, servletContext);
        }

        AbstractHandlerMapping handlerMapping() {
            return super.getHandlerMapping();
        }
    }

    @Test
    void fallsBackOnlyForClientSideRoutes() {
        assertTrue(SpaFallbackConfig.shouldFallback("home"));
        assertTrue(SpaFallbackConfig.shouldFallback("movies"));
        assertTrue(SpaFallbackConfig.shouldFallback("movies/tt0123456"));
        assertTrue(SpaFallbackConfig.shouldFallback("stars/nm0000001"));
        assertTrue(SpaFallbackConfig.shouldFallback("cart"));
        assertTrue(SpaFallbackConfig.shouldFallback("checkout"));
        assertTrue(SpaFallbackConfig.shouldFallback("confirmation"));
        assertTrue(SpaFallbackConfig.shouldFallback("login"));
        assertTrue(SpaFallbackConfig.shouldFallback("employee/login"));
        assertTrue(SpaFallbackConfig.shouldFallback("employee/dashboard"));
        assertTrue(SpaFallbackConfig.shouldFallback("playground"));
        assertTrue(SpaFallbackConfig.shouldFallback("/missing-page"));

        assertFalse(SpaFallbackConfig.shouldFallback(null));
        assertFalse(SpaFallbackConfig.shouldFallback(""));
        assertFalse(SpaFallbackConfig.shouldFallback("/"));
        assertFalse(SpaFallbackConfig.shouldFallback("api"));
        assertFalse(SpaFallbackConfig.shouldFallback("api/boot/unknown"));
        assertFalse(SpaFallbackConfig.shouldFallback("api/boot/cart"));
        assertFalse(SpaFallbackConfig.shouldFallback("error"));
        assertFalse(SpaFallbackConfig.shouldFallback("error/404"));
        assertFalse(SpaFallbackConfig.shouldFallback("assets/index-abc123.js"));
        assertFalse(SpaFallbackConfig.shouldFallback("favicon.ico"));
        assertFalse(SpaFallbackConfig.shouldFallback("assets/missing.js"));

        // The decision is case-insensitive so /API/... and /Error never get HTML.
        assertFalse(SpaFallbackConfig.shouldFallback("API"));
        assertFalse(SpaFallbackConfig.shouldFallback("API/boot/unknown"));
        assertFalse(SpaFallbackConfig.shouldFallback("Api/Boot/Cart"));
        assertFalse(SpaFallbackConfig.shouldFallback("Error"));
        assertFalse(SpaFallbackConfig.shouldFallback("Error/404"));
    }

    @Test
    void servesRealAssetsAndTheSpaEntryDocument(@TempDir Path staticDir) throws IOException {
        Files.writeString(staticDir.resolve("index.html"), "<div id=\"root\"></div>");
        Files.createDirectories(staticDir.resolve("assets"));
        Files.writeString(staticDir.resolve("assets/app.js"), "console.log('app')");
        Resource location = new UrlResource(staticDir.toUri());
        SpaFallbackConfig.SpaResourceResolver resolver = new SpaFallbackConfig.SpaResourceResolver();

        Resource asset = resolver.getResource("assets/app.js", location);
        assertNotNull(asset);
        assertEquals("app.js", asset.getFilename());

        Resource index = resolver.getResource("home", location);
        assertNotNull(index);
        assertEquals("index.html", index.getFilename());
    }

    @Test
    void missingAssetsAndApiPathsAreNeverServedAsHtml(@TempDir Path staticDir) throws IOException {
        Files.writeString(staticDir.resolve("index.html"), "<div id=\"root\"></div>");
        Resource location = new UrlResource(staticDir.toUri());
        SpaFallbackConfig.SpaResourceResolver resolver = new SpaFallbackConfig.SpaResourceResolver();

        assertNull(resolver.getResource("assets/missing.js", location));
        assertNull(resolver.getResource("api/boot/unknown", location));
        assertNull(resolver.getResource("api/boot/cart", location));
        assertNull(resolver.getResource("error", location));
    }

    @Test
    void returnsNothingWhenTheEntryDocumentIsMissing(@TempDir Path staticDir) throws IOException {
        // Default (no-profile) builds ship no static resources, so a SPA route
        // must resolve to null instead of an exception.
        Resource location = new UrlResource(staticDir.toUri());
        SpaFallbackConfig.SpaResourceResolver resolver = new SpaFallbackConfig.SpaResourceResolver();

        assertNull(resolver.getResource("home", location));
    }

    @Test
    void wiresTheCatchAllPatternToTheSpaResolver() {
        // A real (unrefreshed) context is needed so the static location resolves
        // to a ClassPathResource instead of a mocked null.
        InspectableRegistry registry = new InspectableRegistry(
                new GenericApplicationContext(), mock(ServletContext.class));

        new SpaFallbackConfig().addResourceHandlers(registry);

        SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) registry.handlerMapping();
        Object handler = mapping.getUrlMap().get("/**");
        assertInstanceOf(ResourceHttpRequestHandler.class, handler);

        ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler;
        assertTrue(resourceHandler.getResourceResolvers().stream()
                        .anyMatch(resolver -> resolver instanceof SpaFallbackConfig.SpaResourceResolver),
                "the /** handler must use the SPA resolver");
        assertTrue(resourceHandler.getLocations().stream()
                        .anyMatch(location -> location instanceof ClassPathResource
                                && "static/".equals(((ClassPathResource) location).getPath())),
                "the /** handler must resolve under classpath:/static/");
    }
}
