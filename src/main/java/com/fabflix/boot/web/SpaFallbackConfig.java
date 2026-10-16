package com.fabflix.boot.web;

import java.io.IOException;
import java.util.Locale;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the production React build from {@code classpath:/static/} and answers
 * client-side routes with {@code index.html}, so deep links and refreshes work
 * under Spring Boot.
 *
 * <p>Real static resources (and every controller, including {@code /api/**})
 * keep their normal handling; only paths that look like SPA routes fall back.
 * Ordered after Boot's default resource handler (which is {@code @Order(0)}) so
 * this {@code /**} registration replaces it. That replacement also drops Boot's
 * other default static locations ({@code classpath:/META-INF/resources/},
 * {@code classpath:/resources/}, {@code classpath:/public/}); this application
 * ships no resources there, and the frontend assets live only under
 * {@code classpath:/static/}.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaFallbackConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                // resourceChain(false) registers the resolver without enabling the
                // resource cache (which would grow without bound).
                .resourceChain(false)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * Decides whether a request path should be answered with the SPA entry
     * document. It is only consulted after the requested file was not found, so
     * existing assets never reach this decision. Anything that looks like an
     * API call, the error dispatch, or a file (contains a dot) is left alone so
     * a missing asset stays a 404 instead of becoming HTML.
     */
    static boolean shouldFallback(String path) {
        if (path == null) return false;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.isEmpty()) return false;
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.equals("error") || lower.startsWith("error/")) return false;
        if (lower.equals("api") || lower.startsWith("api/")) return false;
        return !lower.contains(".");
    }

    static final class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = super.getResource(resourcePath, location);
            if (requested != null) return requested;
            if (!shouldFallback(resourcePath)) return null;
            // Resolve index.html through the same (traversal-checked) location.
            return super.getResource("index.html", location);
        }
    }
}
