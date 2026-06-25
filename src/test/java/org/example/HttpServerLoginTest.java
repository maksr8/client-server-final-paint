package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.example.network.JwtAuthenticator;
import org.example.network.PaintHttpServer;
import org.example.repository.DrawingRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.DrawingService;
import org.example.service.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class HttpServerLoginTest extends BasePostgresqlTest {

    private static PaintHttpServer httpServer;
    private static ExecutorService executorService;
    private static AuthService authService;

    @BeforeAll
    static void startServer() throws Exception {
        UserRepository userRepository = new UserRepository(connectionProvider);
        JwtService jwtService = new JwtService("secret", "issuer", 3600000L);
        authService = new AuthService(userRepository, jwtService);
        JwtAuthenticator authenticator = new JwtAuthenticator(jwtService);
        DrawingRepository drawingRepository = new DrawingRepository(connectionProvider);
        DrawingService drawingService = new DrawingService(drawingRepository, userRepository);

        executorService = Executors.newFixedThreadPool(2);
        httpServer = new PaintHttpServer(0, executorService, authService, drawingService, authenticator);
        httpServer.start();

        RestAssured.port = httpServer.getPort();

        try {
            authService.registerUser("loginUser", "password123");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @AfterAll
    static void stopServer() {
        if (httpServer != null) {
            httpServer.close();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    void testLoginShouldReturnTokenForValidCredentials() {
        String requestBody = """
                {
                    "username": "loginUser",
                    "password": "password123"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/api/login");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("token")).isNotBlank();
    }

    @Test
    void testLoginShouldReturnUnauthorizedForWrongPassword() {
        String requestBody = """
                {
                    "username": "loginUser",
                    "password": "wrongPassword"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/api/login");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error")).isNotBlank();
    }

    @Test
    void testLoginShouldReturnUnauthorizedForNonExistentUser() {
        String requestBody = """
                {
                    "username": "nonExistentUser",
                    "password": "password123"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/api/login");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error")).isNotBlank();
    }

    @Test
    void testLoginShouldReturnMethodNotAllowedForGetRequest() {
        Response response = given()
                .get("/api/login");

        assertThat(response.statusCode()).isEqualTo(405);
    }
}
