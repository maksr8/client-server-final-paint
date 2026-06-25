package org.example;

import io.restassured.RestAssured;
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

class HttpServerStaticFileTest extends BasePostgresqlTest {
    private static PaintHttpServer httpServer;
    private static ExecutorService executorService;

    @BeforeAll
    static void startServer() throws Exception {
        UserRepository userRepository = new UserRepository(connectionProvider);
        JwtService jwtService = new JwtService("secret", "issuer", 3600000L);
        AuthService authService = new AuthService(userRepository, jwtService);
        JwtAuthenticator authenticator = new JwtAuthenticator(jwtService);
        DrawingRepository drawingRepository = new DrawingRepository(connectionProvider);
        DrawingService drawingService = new DrawingService(drawingRepository, userRepository);

        executorService = Executors.newFixedThreadPool(2);
        httpServer = new PaintHttpServer(0, executorService, authService, drawingService, authenticator);
        httpServer.start();

        RestAssured.port = httpServer.getPort();
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
    void testGetRootShouldReturnIndexHtml() {
        Response response = given().get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).contains("text/html");
        assertThat(response.getBody().asString()).contains("<html");
    }

    @Test
    void testGetAppJsShouldReturnJavascriptFile() {
        Response response = given().get("/app.js");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).contains("application/javascript");
        assertThat(response.getBody().asString()).contains("function");
    }

    @Test
    void testGetNonExistentRouteShouldFallbackToIndexHtml() {
        Response response = given().get("/some-random-frontend-route");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).contains("text/html");
        assertThat(response.getBody().asString()).contains("<html");
    }

    @Test
    void testPathTraversalAttemptShouldReturnForbidden() {
        Response response = given().get("/../../");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.getBody().asString()).contains("Forbidden");
    }
}
