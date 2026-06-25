package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;

import java.util.HexFormat;

public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public String registerUser(String username, String rawPassword) {
        try {
            byte[] hashedBytes = CipherService.generateSHA256FromPassword(rawPassword);
            String passwordHash = HexFormat.of().formatHex(hashedBytes);
            userRepository.createUser(new User(null, username, passwordHash));
            return jwtService.generateToken(username);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error registering user", e);
        }
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String inputPasswordHash;
        try {
            byte[] hashedBytes = CipherService.generateSHA256FromPassword(password);
            inputPasswordHash = HexFormat.of().formatHex(hashedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generating password hash", e);
        }

        if (!user.passwordHash().equals(inputPasswordHash)) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return jwtService.generateToken(username);
    }
}