package org.example;

import org.example.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private JwtService jwtService;
    private static final String SECRET = "mySecretKey";
    private static final String ISSUER = "PaintOnline";
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ISSUER, EXPIRATION_MS);
    }

    @Test
    void testGenerateTokenShouldReturnValidTokenString() {
        String token = jwtService.generateToken("testUser");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void testValidateTokenAndGetUsernameShouldReturnCorrectUsernameForValidToken() {
        String username = "testUser";
        String token = jwtService.generateToken(username);

        String extractedUsername = jwtService.validateTokenAndGetUsername(token);

        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void testValidateTokenAndGetUsernameShouldReturnNullForInvalidTokenFormat() {
        String invalidToken = "invalid.token.format";

        String extractedUsername = jwtService.validateTokenAndGetUsername(invalidToken);

        assertThat(extractedUsername).isNull();
    }

    @Test
    void testValidateTokenAndGetUsernameShouldReturnNullForTokenSignedWithDifferentSecret() {
        JwtService otherJwtService = new JwtService("differentSecret", ISSUER, EXPIRATION_MS);
        String token = otherJwtService.generateToken("testUser");

        String extractedUsername = jwtService.validateTokenAndGetUsername(token);

        assertThat(extractedUsername).isNull();
    }

    @Test
    void testValidateTokenAndGetUsernameShouldReturnNullForExpiredToken() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, ISSUER, 1L);
        String token = shortLivedJwtService.generateToken("testUser");

        Thread.sleep(50);

        String extractedUsername = shortLivedJwtService.validateTokenAndGetUsername(token);

        assertThat(extractedUsername).isNull();
    }
}
