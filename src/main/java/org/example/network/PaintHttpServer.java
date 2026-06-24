package org.example.network;

import com.sun.net.httpserver.HttpServer;
import org.example.service.AuthService;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

public class PaintHttpServer implements AutoCloseable {
    private final HttpServer server;

    public PaintHttpServer(int port, ExecutorService executor, AuthService authService, JwtAuthenticator authenticator) throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);
        server.createContext("/login", new LoginHandler(authService));
        //var productContext = server.createContext("/products", new ProductHandler(itemService));
        //productContext.setAuthenticator(authenticator);
        this.server.createContext("/", new StaticFileHandler("src/main/resources/static"));
    }

    public void start() {
        server.start();
        System.out.println("HTTP Server is listening on port " + server.getAddress().getPort());
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(2);
            System.out.println("HTTP Server stopped.");
        }
    }
}