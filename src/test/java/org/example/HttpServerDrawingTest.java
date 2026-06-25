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

class HttpServerDrawingTest extends BasePostgresqlTest {

    private static PaintHttpServer httpServer;
    private static ExecutorService executorService;
    private static String validToken;

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

        authService.registerUser("drawingUser", "password123");
        validToken = authService.login("drawingUser", "password123");
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
    void testCreateAndGetDrawingShouldSucceedWithValidToken() {
        String createBody = """
                {
                    "name": "My New Drawing"
                }
                """;

        Response createResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(createBody)
                .post("/api/drawings");

        assertThat(createResponse.statusCode()).isEqualTo(201);
        String drawingId = createResponse.jsonPath().getString("id");
        assertThat(drawingId).isNotBlank();
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo("My New Drawing");

        Response getResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .get("/api/drawings/" + drawingId);

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getString("id")).isEqualTo(drawingId);
        assertThat(getResponse.jsonPath().getString("name")).isEqualTo("My New Drawing");
    }

    @Test
    void testGetAllDrawingsShouldReturnList() {
        given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body("{\"name\": \"List Drawing\"}")
                .post("/api/drawings");

        Response response = given()
                .header("Authorization", "Bearer " + validToken)
                .get("/api/drawings");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("owned")).isNotEmpty();
    }

    @Test
    void testUpdateDrawingDataShouldSucceed() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Data Drawing\"}")
                .post("/api/drawings");

        String drawingId = createResponse.jsonPath().getString("id");
        String updateBody = """
                {
                    "data": "[{\\"x\\": 10, \\"y\\": 20}]"
                }
                """;

        Response updateResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(updateBody)
                .put("/api/drawings/" + drawingId);

        assertThat(updateResponse.statusCode()).isEqualTo(200);

        Response getResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .get("/api/drawings/" + drawingId);

        assertThat(getResponse.jsonPath().getString("data")).isEqualTo("[{\"x\": 10, \"y\": 20}]");
    }

    @Test
    void testRenameDrawingShouldUpdateName() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body("{\"name\": \"Old Name\"}")
                .post("/api/drawings");

        String drawingId = createResponse.jsonPath().getString("id");
        String renameBody = """
                {
                    "name": "New Name"
                }
                """;

        Response patchResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body(renameBody)
                .patch("/api/drawings/" + drawingId + "/name");

        assertThat(patchResponse.statusCode()).isEqualTo(200);

        Response getResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .get("/api/drawings/" + drawingId);

        assertThat(getResponse.jsonPath().getString("name")).isEqualTo("New Name");
    }

    @Test
    void testDeleteDrawingShouldRemoveIt() {
        Response createResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .contentType(ContentType.JSON)
                .body("{\"name\": \"To Delete\"}")
                .post("/api/drawings");

        String drawingId = createResponse.jsonPath().getString("id");

        Response deleteResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .delete("/api/drawings/" + drawingId);

        assertThat(deleteResponse.statusCode()).isEqualTo(200);

        Response getResponse = given()
                .header("Authorization", "Bearer " + validToken)
                .get("/api/drawings/" + drawingId);

        assertThat(getResponse.statusCode()).isNotEqualTo(200);
    }

    @Test
    void testUnauthorizedRequestShouldFail() {
        Response response = given()
                .get("/api/drawings");

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
