package cg.xis.eradio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage refresh tokens
 *
 * In-memory storage for simplicity. In production, consider using Redis or database.
 */
@Service
@Slf4j
public class RefreshTokenService {

    // Store refresh tokens: token -> username
    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();

    /**
     * Store a refresh token for a user
     */
    public void storeRefreshToken(String token, String username) {
        refreshTokens.put(token, username);
        log.debug("Stored refresh token for user: {}", username);
    }

    /**
     * Validate and retrieve username for a refresh token
     */
    public String validateAndGetUsername(String token) {
        return refreshTokens.get(token);
    }

    /**
     * Remove a refresh token (e.g., on logout)
     */
    public void removeRefreshToken(String token) {
        refreshTokens.remove(token);
        log.debug("Removed refresh token");
    }

    /**
     * Remove all refresh tokens for a user (e.g., on password change)
     */
    public void removeAllTokensForUser(String username) {
        refreshTokens.entrySet().removeIf(entry -> entry.getValue().equals(username));
        log.debug("Removed all refresh tokens for user: {}", username);
    }
}

