package com.beesagono.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility component responsible for generating, parsing, and validating JWT
 * access tokens.
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final Key signingKey;
    private final long jwtExpirationMs;

    /**
     * Constructs the JWT utility component using application configuration
     * properties.
     *
     * @param secret          secret key used to sign tokens
     * @param jwtExpirationMs token lifespan in milliseconds
     */
    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long jwtExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generates a signed JWT access token containing subject claims and assigned
     * user roles.
     *
     * @param userPrincipal authenticated user security principal
     * @return signed compact JWT string
     */
    public String generateJwtToken(UserDetailsImpl userPrincipal) {
        Instant now = Instant.now();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(jwtExpirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the username subject from a valid JWT.
     *
     * @param token raw JWT string
     * @return extracted username
     */
    public String getUsernameFromJwtToken(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the unique user identifier claim from a valid JWT.
     *
     * @param token raw JWT string
     * @return extracted user ID
     */
    public String getUserIdFromJwtToken(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    /**
     * Extracts the expiration instant from a valid JWT.
     *
     * @param token raw JWT string
     * @return token expiration instant
     */
    public Instant extractExpiry(String token) {
        return extractClaims(token).getExpiration().toInstant();
    }

    /**
     * Parses claims from a signed JWT string.
     *
     * @param token raw JWT string
     * @return parsed {@link Claims}
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates a JWT against structure, signature, and expiration rules.
     *
     * @param authToken raw JWT string to evaluate
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException e) {
            logger.error("Firma JWT non valida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Token JWT malformato: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Token JWT scaduto: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT non supportato: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("La stringa dei claims del JWT è vuota: {}", e.getMessage());
        }
        return false;
    }
}