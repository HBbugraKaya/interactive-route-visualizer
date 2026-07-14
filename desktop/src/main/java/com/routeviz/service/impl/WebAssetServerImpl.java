package com.routeviz.service.impl;

import com.routeviz.service.WebAssetServer;
import com.routeviz.service.WebAssetServerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;

public final class WebAssetServerImpl implements WebAssetServer {
    private static final Logger log = LoggerFactory.getLogger(WebAssetServerImpl.class);

    private final HttpServer server;
    private final String baseUrl;

    private WebAssetServerImpl(HttpServer server, int port) {
        this.server = server;
        this.baseUrl = "http://127.0.0.1:" + port + "/";
    }

    public static WebAssetServerFactory factory() {
        return WebAssetServerImpl::start;
    }

    public static WebAssetServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", WebAssetServerImpl::handle);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "routeviz-web");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        int port = server.getAddress().getPort();
        log.info("Web asset server listening on http://127.0.0.1:{}/", port);
        return new WebAssetServerImpl(server, port);
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    private static void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            path = "/index.html";
        }

        String resourcePath = "web" + path;
        if (resourcePath.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        try (InputStream in = WebAssetServerImpl.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                byte[] body = ("Not found: " + path).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
                return;
            }

            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentTypeFor(path));
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            } else {
                exchange.close();
            }
        }
    }

    static String contentTypeFor(String path) {
        if (path.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (path.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        return Objects.requireNonNullElse(
                URLConnection.guessContentTypeFromName(path),
                "application/octet-stream");
    }

    @Override
    public void close() {
        server.stop(0);
        log.info("Web asset server stopped ({})", baseUrl);
    }
}
