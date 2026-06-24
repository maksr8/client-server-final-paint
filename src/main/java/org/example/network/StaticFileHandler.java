package org.example.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private final String baseDir;

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.contains("..")) {
            sendResponse(exchange, 403, "Forbidden", "text/plain");
            return;
        }

        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(baseDir + path);

        if (!file.exists() || file.isDirectory()) {
            file = new File(baseDir + "/index.html");
            if (!file.exists()) {
                sendResponse(exchange, 404, "Frontend not found", "text/plain");
                return;
            }
        }

        String contentType = "text/plain";
        if (file.getName().endsWith(".html")) contentType = "text/html";
        else if (file.getName().endsWith(".css")) contentType = "text/css";
        else if (file.getName().endsWith(".js")) contentType = "application/javascript";
        else if (file.getName().endsWith(".png")) contentType = "image/png";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}