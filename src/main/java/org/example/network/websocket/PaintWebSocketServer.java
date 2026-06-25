package org.example.network.websocket;

import org.example.service.JwtService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PaintWebSocketServer extends WebSocketServer {
    private final JwtService jwtService;
    
    private final Map<String, Set<WebSocket>> drawingConnections = new ConcurrentHashMap<>();

    public PaintWebSocketServer(int port, JwtService jwtService) {
        super(new InetSocketAddress(port));
        this.jwtService = jwtService;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            URI uri = new URI(handshake.getResourceDescriptor());
            String path = uri.getPath();
            String query = uri.getQuery();

            if (query == null || !query.startsWith("token=")) {
                conn.closeConnection(4001, "Missing token");
                return;
            }
            String token = query.substring(6);
            String username = jwtService.validateTokenAndGetUsername(token);
            if (username == null) {
                conn.closeConnection(4001, "Invalid or expired token");
                return;
            }

            if (!path.startsWith("/drawing/")) {
                conn.closeConnection(4002, "Invalid path");
                return;
            }
            String drawingId = path.substring(9);
            conn.setAttachment(drawingId);

            drawingConnections
                    .computeIfAbsent(drawingId, k -> ConcurrentHashMap.newKeySet())
                    .add(conn);
            System.out.println("User [" + username + "] joined drawing ID: " + drawingId);
        } catch (Exception e) {
            conn.closeConnection(5000, "Internal Server Error");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String drawingId = conn.getAttachment();
        if (drawingId != null) {
            Set<WebSocket> clients = drawingConnections.get(drawingId);
            if (clients != null) {
                clients.remove(conn);
                if (clients.isEmpty()) {
                    drawingConnections.remove(drawingId);
                }
            }
            System.out.println("Connection closed for drawing ID: " + drawingId);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        String drawingId = conn.getAttachment();
        if (drawingId == null) return;

        Set<WebSocket> clients = drawingConnections.getOrDefault(drawingId, Collections.emptySet());
        
        for (WebSocket client : clients) {
            if (client != conn && client.isOpen()) {
                client.send(message); 
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server started on port: " + getPort());
    }
}