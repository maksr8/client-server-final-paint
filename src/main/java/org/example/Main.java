package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.network.JwtAuthenticator;
import org.example.network.PaintHttpServer;
import org.example.repository.ConnectionProvider;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.JwtService;
import org.flywaydb.core.Flyway;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        System.out.println("Loading environment variables...");
        Dotenv dotenv = Dotenv.load();
        String dbUrl = dotenv.get("DB_URL");
        String dbUser = dotenv.get("DB_USER");
        String dbPass = dotenv.get("DB_PASSWORD");
        int httpPort = Integer.parseInt(dotenv.get("HTTP_PORT"));
        String jwtSecret = dotenv.get("JWT_SECRET");
        String jwtIssuer = dotenv.get("JWT_ISSUER");
        long jwtExpirationTimeMs = Long.parseLong(dotenv.get("JWT_EXPIRATION_TIME_MS"));

        System.out.println("Applying database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPass)
                .load();
        flyway.migrate();
        System.out.println("Migrations applied successfully!");

        try {
            ConnectionProvider connectionProvider = new ConnectionProvider(dbUrl, dbUser, dbPass);
            UserRepository userRepository = new UserRepository(connectionProvider);
            JwtService jwtService = new JwtService(jwtSecret, jwtIssuer, jwtExpirationTimeMs);
            AuthService authService = new AuthService(userRepository, jwtService);
            JwtAuthenticator authenticator = new JwtAuthenticator(jwtService);

            authService.registerUser("admin", "admin123");

            ExecutorService serverExecutor = Executors.newFixedThreadPool(10);
            PaintHttpServer httpServer = new PaintHttpServer(httpPort, serverExecutor, authService, authenticator);
            httpServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                httpServer.close();
                serverExecutor.shutdown();
                connectionProvider.close();
            }));
        } catch (Exception e) {
            System.err.println("Failed to start the server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}