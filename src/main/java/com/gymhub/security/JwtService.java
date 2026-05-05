package com.gymhub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility — mirrors the farmer project's JwtUtil:
 * generates / validates tokens using the user's email as the subject.
 *
 * Two overloads for {@code isTokenValid} are provided:
 * <ul>
 *   <li>{@code isTokenValid(token, email)} — used by the JWT filter (works with the plain User entity)</li>
 *   <li>{@code isTokenValid(token, UserDetails)} — used by the AuthenticationProvider / tests</li>
 * </ul>
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Token generation ──────────────────────────────────────────────────────

    /** Generate an access token from a Spring {@link UserDetails} (used during login). */
    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), jwtExpirationMs);
    }

    /** Generate an access token directly from the user's email and role string. */
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return buildToken(claims, email, jwtExpirationMs);
    }

    /** Generate a refresh token. */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), refreshExpirationMs);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token validation ──────────────────────────────────────────────────────

    /**
     * Validate a token against a plain email string.
     * Used by {@link JwtAuthenticationFilter} which holds the User entity directly.
     */
    public boolean isTokenValid(String token, String email) {
        return email.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    /**
     * Validate a token against a {@link UserDetails} object.
     * Used by the {@code DaoAuthenticationProvider} and tests.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails.getUsername());
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Claims extraction ─────────────────────────────────────────────────────

    /** Returns the email address stored as the JWT subject. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
