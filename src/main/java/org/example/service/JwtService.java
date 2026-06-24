package org.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

public class JwtService {
    private final String issuer;
    private final long expirationTimeMs;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(String secretKey, String issuer, long expirationTimeMs) {
        this.issuer = issuer;
        this.expirationTimeMs = expirationTimeMs;
        algorithm = Algorithm.HMAC256(secretKey);
        verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String generateToken(String username) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTimeMs))
                .sign(algorithm);
    }

    public String validateTokenAndGetUsername(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}