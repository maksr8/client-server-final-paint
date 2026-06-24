package org.example.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.service.AuthService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

public class RegisterHandler implements HttpHandler {
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            LoginRequest req = objectMapper.readValue(exchange.getRequestBody(), LoginRequest.class);

            if (req.username() == null || req.username().isBlank() ||
                req.password() == null || req.password().isBlank()) {
                sendError(exchange, 400, "Username and password cannot be empty");
                return;
            }

            String token = authService.registerUser(req.username(), req.password());

            String responseJson = objectMapper.writeValueAsString(new LoginResponse(token));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseJson.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseJson.getBytes());
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error");
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorJson = "{\"error\": \"" + message + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorJson.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorJson.getBytes());
        }
    }
}