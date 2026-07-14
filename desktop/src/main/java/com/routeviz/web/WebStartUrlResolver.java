package com.routeviz.web;

import com.routeviz.service.WebAssetServer;
import com.routeviz.service.WebAssetServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class WebStartUrlResolver {
    private static final Logger log = LoggerFactory.getLogger(WebStartUrlResolver.class);

    public record Resolution(String url, WebAssetServer assetServer) {
    }

    public Resolution resolve(WebAssetServerFactory webAssetServerFactory, Consumer<String> onStatus) {
        String env = System.getenv("ROUTEVIZ_WEB_URL");
        if (env != null && !env.isBlank()) {
            log.info("Using ROUTEVIZ_WEB_URL={}", env);
            return new Resolution(env, null);
        }
        String prop = System.getProperty("routeviz.web.url");
        if (prop != null && !prop.isBlank()) {
            log.info("Using -Drouteviz.web.url={}", prop);
            return new Resolution(prop, null);
        }

        var resource = WebViewHost.class.getResource("/web/index.html");
        if (resource != null) {
            try {
                WebAssetServer server = webAssetServerFactory.start();
                return new Resolution(server.baseUrl(), server);
            } catch (Exception e) {
                log.error("Failed to start web asset server", e);
                if (onStatus != null) {
                    onStatus.accept("Local web server failed: " + e.getMessage());
                }
            }
        }

        Path dist = Path.of("web", "dist", "index.html");
        if (dist.toFile().exists()) {
            String uri = dist.toAbsolutePath().toUri().toString();
            log.info("Using local dist {}", uri);
            return new Resolution(uri, null);
        }

        log.info("Falling back to Vite http://localhost:5173");
        return new Resolution("http://localhost:5173", null);
    }
}
