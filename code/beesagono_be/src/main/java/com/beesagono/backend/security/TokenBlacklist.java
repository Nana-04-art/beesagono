package com.beesagono.backend.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe blacklist service for invalidating revoked or
 * logged-out JWT tokens.
 */
@Component
public class TokenBlacklist {

    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Adds a raw token to the blacklist with its corresponding expiration
     * timestamp.
     *
     * @param token  the JWT string to blacklist
     * @param expiry the instant when the token expires
     */
    public void add(String token, Instant expiry) {
        blacklistedTokens.put(token, expiry);
    }

    /**
     * Checks whether a raw token has been added to the blacklist.
     *
     * @param token raw JWT string to verify
     * @return {@code true} if the token is blacklisted, {@code false} otherwise
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    /**
     * Purges expired tokens from the blacklist to prevent unbounded memory
     * consumption.
     */
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}