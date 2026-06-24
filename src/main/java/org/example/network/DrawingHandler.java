package org.example.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dto.CreateDrawingRequest;
import org.example.model.Drawing;
import org.example.service.DrawingService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class DrawingHandler implements HttpHandler {
    private final DrawingService drawingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DrawingHandler(DrawingService drawingService) {
        this.drawingService = drawingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");
        String username = exchange.getPrincipal().getUsername();

        try {
            if (pathParts.length == 3 && method.equals("GET")) {
                handleGetAll(exchange);
            } else if (pathParts.length == 3 && method.equals("POST")) {
                handleCreate(exchange, username);
            } else if (pathParts.length == 4) {
                int drawingId = Integer.parseInt(pathParts[3]);
                
                switch (method) {
                    case "GET" -> handleGetOne(exchange, drawingId);
                    case "PUT" -> handleUpdateData(exchange, drawingId);
                    case "DELETE" -> handleDelete(exchange, username, drawingId);
                    default -> sendResponse(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
                }
            } else {
                sendResponse(exchange, 404, "{\"error\": \"Endpoint not found\"}");
            }
        } catch (SecurityException e) {
            sendResponse(exchange, 403, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Internal Server Error\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        List<Drawing> drawings = drawingService.getAllDrawings();
        sendResponse(exchange, 200, objectMapper.writeValueAsString(drawings));
    }

    private void handleGetOne(HttpExchange exchange, int id) throws IOException {
        Drawing drawing = drawingService.getDrawing(id);
        sendResponse(exchange, 200, objectMapper.writeValueAsString(drawing));
    }

    private void handleCreate(HttpExchange exchange, String username) throws IOException {
        CreateDrawingRequest req = objectMapper.readValue(exchange.getRequestBody(), CreateDrawingRequest.class);
        Drawing newDrawing = drawingService.createDrawing(username, req.name());
        sendResponse(exchange, 201, objectMapper.writeValueAsString(newDrawing));
    }

    private void handleUpdateData(HttpExchange exchange, int id) throws IOException {
        var jsonNode = objectMapper.readTree(exchange.getRequestBody());
        String data = jsonNode.get("data").asString();
        
        drawingService.saveDrawingData(id, data);
        sendResponse(exchange, 200, "{\"message\": \"Saved successfully\"}");
    }

    private void handleDelete(HttpExchange exchange, String username, int id) throws IOException {
        drawingService.deleteDrawing(username, id);
        sendResponse(exchange, 200, "{\"message\": \"Deleted successfully\"}");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseJson.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseJson.getBytes());
        }
    }
}