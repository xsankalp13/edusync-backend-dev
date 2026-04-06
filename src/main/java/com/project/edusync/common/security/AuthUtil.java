package com.project.edusync.common.security;

import com.project.edusync.common.exception.iam.InsufficientAuthenticationException;
import com.project.edusync.iam.model.entity.Permission;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.lang.Collections;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthUtil {

    /**
     * Retrieves the full User entity of the currently authenticated user.
     *
     * @return The authenticated User entity.
     * @throws InsufficientAuthenticationException if no user is authenticated or is anonymous.
     */
    public User getCurrentUser() {
        log.trace("Attempting to retrieve current authenticated user.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Authentication check failed: No authentication object found or user is not authenticated.");
            throw new InsufficientAuthenticationException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            log.trace("Successfully retrieved authenticated user: {}", user.getUsername());
            return user;
        }

        if ("anonymousUser".equals(principal)) {
            log.warn("Authentication check failed: User is anonymous.");
            throw new InsufficientAuthenticationException("User is anonymous.");
        }

        // If the principal is not what we expect, it's a critical configuration error.
        String principalType = (principal == null ? "null" : principal.getClass().getName());
        log.error("CRITICAL CONFIGURATION ERROR: Authenticated principal is not of the expected type 'User'. Found: {}. Please ensure your CustomUserDetailService returns the 'com.project.edusync.iam.model.entity.User' object.", principalType);
        throw new InsufficientAuthenticationException("Authenticated principal is not of the expected type 'User'. " +
                "Found: " + principalType +
                ". Please ensure your CustomUserDetailService returns the 'com.project.edusync.iam.model.entity.User' object.");
    }

    /**
     * A convenience method to retrieve the user ID of the currently authenticated user.
     *
     * @return The Long user ID of the authenticated user.
     * @throws InsufficientAuthenticationException if no user is authenticated.
     */
    public Long getCurrentUserId() {
        log.trace("Retrieving user ID for current user.");
        return getCurrentUser().getId();
    }

    /**
     * Retrieves the academic year ID from Authentication details injected by JWTFilter.
     * Returns null when the token/context does not carry this claim.
     */
    public Long getCurrentAcademicYearId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("User is not authenticated.");
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Map<?, ?> detailsMap)) {
            return null;
        }

        Object academicYearId = detailsMap.get("academic_year_id");
        if (academicYearId == null) {
            return null;
        }

        if (academicYearId instanceof Number number) {
            return number.longValue();
        }

        String raw = Objects.toString(academicYearId, null);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            log.warn("Invalid academic_year_id in auth context: {}", raw);
            return null;
        }
    }

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    @Value("${app.jwt.expirationTime}")
    private long jwtExpirationTime;

    @Value("${app.jwt.refresh-expirationTime}")
    private long jwtRefreshExpirationTime;

    private SecretKey _signingKey;

    @PostConstruct
    public void init() {
        this._signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        log.info("JWT signing key initialized successfully.");
    }

    private SecretKey getSigningKey() {
        return this._signingKey;
    }

    /**
     * Generates a short-lived Access Token containing user authorities.
     *
     * @param username The user's username (subject).
     * @param roles    The user's roles.
     * @return A signed JWT Access Token.
     */
    public String generateAccessToken(String username, Set<Role> roles) {
        return generateAccessToken(username, roles, null, null);
    }

    /**
     * Generates a short-lived Access Token containing authorities and context claims.
     */
    public String generateAccessToken(String username, Set<Role> roles, Long userId, Long academicYearId) {
        log.trace("Generating access token for user: {}", username);

        // Include role + permission authorities so method-level checks can use either.
        Set<String> authoritySet = new LinkedHashSet<>();
        roles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(authoritySet::add);

        roles.stream()
                .filter(role -> role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(authoritySet::add);

        List<String> authorityStrings = authoritySet.stream().toList();
        log.trace("Included {} authorities in access token for user: {}", authorityStrings.size(), username);

        // 2. Create a 'claims' map to store the authorities
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorityStrings);
        claims.put("user_id", userId);
        claims.put("academic_year_id", academicYearId);

        // 3. Build the token
        String token = Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationTime))
                .claims(claims)
                .signWith(getSigningKey())
                .compact();

        log.trace("Access token generated successfully for user: {}.", username);
        return token;
    }

    /**
     * Extracts all claims from the token using the secure key.
     */
    private Claims getAllClaimsFromToken(String token) {
        log.trace("Parsing all claims from token.");
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the username (subject) from the token.
     */
    public String getUsernameFromToken(String token) {
        log.trace("Extracting username from token.");
        String username = getAllClaimsFromToken(token).getSubject();
        log.trace("Extracted username '{}' from token.", username);
        return username;
    }

    /**
     * Extracts authorities from the token's "authorities" claim.
     */
    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        log.trace("Extracting authorities from token.");
        Claims claims = getAllClaimsFromToken(token);

        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) claims.get("authorities");

        if (authorities == null || authorities.isEmpty()) {
            log.trace("No 'authorities' claim found in token or claim is empty.");
            return Collections.emptyList();
        }

        log.trace("Found {} authorities in token.", authorities.size());
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Reads a claim value from a signed JWT token.
     */
    public Object getClaimValueFromToken(String token, String claimName) {
        return getAllClaimsFromToken(token).get(claimName);
    }

    /**
     * --- NEW ---
     * Validates a token by attempting to parse it.
     *
     * @param token The JWT token to validate.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token) {
        log.trace("Validating token.");
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            log.trace("Token validated successfully.");
            return true;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        log.warn("Token validation failed.");
        return false;
    }
}