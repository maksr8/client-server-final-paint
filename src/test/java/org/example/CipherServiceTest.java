package org.example;

import org.example.service.CipherService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

class CipherServiceTest {

    @Test
    void testGenerateSHA256FromPasswordShouldReturnCorrectHash() throws NoSuchAlgorithmException {
        String password = "mySecretPassword";
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = sha256.digest(password.getBytes(StandardCharsets.UTF_8));

        byte[] actualHash = CipherService.generateSHA256FromPassword(password);

        assertThat(actualHash)
                .isNotEmpty()
                .isEqualTo(expectedHash);
    }

    @Test
    void testGenerateSHA256FromPasswordShouldReturnDifferentHashesForDifferentPasswords() throws NoSuchAlgorithmException {
        String passwordOne = "password123";
        String passwordTwo = "Password123";

        byte[] hashOne = CipherService.generateSHA256FromPassword(passwordOne);
        byte[] hashTwo = CipherService.generateSHA256FromPassword(passwordTwo);

        assertThat(hashOne).isNotEqualTo(hashTwo);
    }

    @Test
    void testGenerateSHA256FromPasswordShouldHandleEmptyString() throws NoSuchAlgorithmException {
        String password = "";
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = sha256.digest(password.getBytes(StandardCharsets.UTF_8));

        byte[] actualHash = CipherService.generateSHA256FromPassword(password);

        assertThat(actualHash).isEqualTo(expectedHash);
    }
}
