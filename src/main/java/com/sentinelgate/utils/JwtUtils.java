package com.sentinelgate.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JwtUtils — generate and validate JWTs that carry an arbitrary Java object.
 *
 * Requirements:
 *  - configUtils.getJwtSecret() should return a BASE64-encoded key of at least 32 bytes (256 bits)
 *    Example: generated via `openssl rand -base64 32`
 *
 * Security policy:
 *  - If secret is missing or too weak, init() will throw IllegalStateException unless
 *    allowEphemeralKeyForDev is set to true (dev only).
 */
@Component
public class JwtUtils {

    @Autowired
    private ConfigUtils configUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private static final String CLAIM_PAYLOAD = "payload";
    private SecretKey signingKey;

    /**
     * If you absolutely want automatic ephemeral keys for local dev/testing, set this to true.
     * WARNING: ephemeral keys are lost on restart and MUST NOT be used in production.
     */
    private final boolean allowEphemeralKeyForDev = false;

    @PostConstruct
    private void init() {
        String secretConfig = configUtils.getJwtSecret();
        if (secretConfig == null || secretConfig.isBlank()) {
            String msg = "jwt.secret is not configured. Provide a BASE64-encoded 256-bit (or larger) key via ConfigUtils (jwt.secret).";
            if (allowEphemeralKeyForDev) {
                log.warn(msg + " Falling back to an ephemeral generated key because allowEphemeralKeyForDev=true (DEV ONLY).");
                signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
                return;
            } else {
                log.error(msg + " See README: generate with 'openssl rand -base64 32' and set jwt.secret=");
                throw new IllegalStateException(msg);
            }
        }

        byte[] keyBytes;
        // try to decode as Base64 first (recommended)
        try {
            keyBytes = Base64.getDecoder().decode(secretConfig);
        } catch (IllegalArgumentException ex) {
            // If not Base64, fall back to raw bytes but warn: raw bytes are less portable/secure.
            log.warn("jwt.secret does not look like Base64; falling back to raw UTF-8 bytes (not recommended).");
            keyBytes = secretConfig.getBytes(StandardCharsets.UTF_8);
        }

        // enforce minimum length (>= 32 bytes == 256 bits) per RFC 7518 for HS256
        if (keyBytes.length < 32) {
            String msg = String.format(
                    "Provided key is %d bytes (%d bits) which is too weak for HS256. Use a key >= 32 bytes (256 bits).",
                    keyBytes.length, keyBytes.length * 8
            );
            // explicit guidance
            msg += " Generate one with: openssl rand -base64 32";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // will throw WeakKeyException internally if still not valid; we allow it to surface as runtime exception
        signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("Jwt signing key initialized successfully (key length = {} bytes).", keyBytes.length);
    }

    public String generateToken(Object object) throws JsonProcessingException {
        if (signingKey == null) throw new IllegalStateException("JWT signing key not initialized");

        String payloadJson = objectMapper.writeValueAsString(object);
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiresAt = new Date(now + configUtils.getJwtExpirationMs());

        return Jwts.builder()
                .claim(CLAIM_PAYLOAD, payloadJson)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public <T> T parseAndGetPayload(String token, Class<T> clazz) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = jws.getBody();
            String payloadJson = claims.get(CLAIM_PAYLOAD, String.class);
            if (payloadJson == null) {
                throw new IllegalArgumentException("Token does not contain payload claim");
            }
            return objectMapper.readValue(payloadJson, clazz);
        } catch (IOException ex) {
            log.error("Failed to deserialize JWT payload to {}: {}", clazz.getSimpleName(), ex.getMessage());
            throw new RuntimeException(ex);
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** Convenience: generate a Base64-encoded random secret (call only from admin / CLI, not at runtime). */
    public static String generateBase64Secret(int keySizeBits) {
        byte[] key = new byte[keySizeBits / 8];
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        rnd.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
