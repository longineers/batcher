package com.longineers.batcher.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test") // picks up application‑test.properties
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testUsername = "alice@example.com";

    /** Helper to force‑inject the @Value fields via reflection. */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Helper to access the private getSigningKey() method via reflection. */
    private Key getPrivateSigningKey() throws Exception {
        Method getSigningKeyMethod = JwtUtil.class.getDeclaredMethod("getSigningKey");
        getSigningKeyMethod.setAccessible(true);
        return (Key) getSigningKeyMethod.invoke(jwtUtil);
    }

    /** Helper to access the private extractAllClaims() method via reflection. */
    private Claims getPrivateExtractAllClaims(String token) throws Exception {
        Method extractAllClaimsMethod = JwtUtil.class.getDeclaredMethod("extractAllClaims", String.class);
        extractAllClaimsMethod.setAccessible(true);
        return (Claims) extractAllClaimsMethod.invoke(jwtUtil, token);
    }

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        // Inject the secret and expiry‑time that the component expects.
        // These match the values defined in application‑test.properties.
        injectField(jwtUtil, "secret", "TestJwtSecretKeyChangeMeThis32CharKey");
        injectField(jwtUtil, "expiryTime", 600_000L); // 10 minutes
    }

    /** --------------------------------------------------------------------
     *  Simple happy‑path test – generate a token and then validate it.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("generateToken → isAuthenticated returns true for same user")
    void generateAndValidateToken() {
        // ── arrange ───────────────────────────────────────
        var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

        // ── act ───────────────────────────────────────────
        String token = jwtUtil.generateToken(userDetails);
        boolean valid = jwtUtil.isAuthenticated(token, userDetails);

        // ── assert ────────────────────────────────────────
        assertThat(token).isNotBlank();
        assertThat(valid).isTrue();
    }

    /** --------------------------------------------------------------------
     *  Extract the username (subject) from a freshly generated token.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("extractUsername returns the subject stored in the token")
    void extractUsername() {
        var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

        String token = jwtUtil.generateToken(userDetails);
        String extracted = jwtUtil.extractUsername(token);

        assertThat(extracted).isEqualTo(testUsername);
    }

    /** --------------------------------------------------------------------
     *  Verify that the expiration claim matches the configured ttl.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("extractExpiration returns a date ~expiry‑time milliseconds in the future")
    void extractExpiration() {
        var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

        String token = jwtUtil.generateToken(userDetails);
        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        long diff = expiration.getTime() - now.getTime();
        // Allow a small drift (a few seconds) because the token is built at runtime.
        assertThat(diff).isBetween(590_000L, 610_000L);
    }

    /** --------------------------------------------------------------------
     *  Token should be considered *not* expired right after creation.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("isTokenExpired returns false for a newly created token")
    void tokenIsNotExpiredImmediately() throws Exception {
        var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

        String token = jwtUtil.generateToken(userDetails);

        // Use reflection to call the private helper (just to prove its logic works)
        // Actually we can just invoke the public isAuthenticated which internally checks expiry.
        boolean valid = jwtUtil.isAuthenticated(token, userDetails);
        assertThat(valid).isTrue();
    }

    /** --------------------------------------------------------------------
     *  Simulate an expired token by manually tweaking the expiration claim.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("isAuthenticated throws ExpiredJwtException when token is expired")
    void tokenIsExpired() throws Exception {
        // Build a token whose expiration is already in the past.
        String expiredToken = Jwts.builder()
                .setSubject(testUsername)
                .setIssuedAt(new Date(System.currentTimeMillis() - 2_000_000L)) // issued ~33 min ago
                .setExpiration(new Date(System.currentTimeMillis() - 1_000_000L)) // expired ~16 min ago
                .signWith(getPrivateSigningKey())
                .compact();

        var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

        // Expect an ExpiredJwtException to be thrown when validating an expired token
        assertThatThrownBy(() -> jwtUtil.isAuthenticated(expiredToken, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    /** --------------------------------------------------------------------
     *  Verify that a token generated for one user does NOT validate against
     *  a different UserDetails instance.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("isAuthenticated fails when usernames differ")
    void tokenFailsForDifferentUser() {
        var alice = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(alice.getUsername()).thenReturn("alice@example.com");

        var bob = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(bob.getUsername()).thenReturn("bob@example.com");

        String token = jwtUtil.generateToken(alice);
        boolean valid = jwtUtil.isAuthenticated(token, bob);
        assertThat(valid).isFalse();
    }

    /** --------------------------------------------------------------------
     *  Bonus sanity check – the raw JWT claims contain the expected keys.
     *  -------------------------------------------------------------------- */
    @Test
    @DisplayName("raw JWT claims contain subject and expiration")
    void rawClaimsContainExpectedFields() throws Exception {
        var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

        String token = jwtUtil.generateToken(userDetails);
        Claims claims = getPrivateExtractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo(testUsername);
        assertThat(claims.getExpiration()).isNotNull();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    //                              MISSING TESTS SECTION
    // ═══════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Invalid Token Handling")
    class InvalidTokenTests {

        @Test
        @DisplayName("extractUsername throws MalformedJwtException for invalid token format")
        void extractUsernameWithMalformedToken() {
            String invalidToken = "this-is-not-a-jwt-token";

            assertThatThrownBy(() -> jwtUtil.extractUsername(invalidToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("extractExpiration throws MalformedJwtException for invalid token format")
        void extractExpirationWithMalformedToken() {
            String invalidToken = "invalid.jwt.token";

            assertThatThrownBy(() -> jwtUtil.extractExpiration(invalidToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("isAuthenticated throws exception for malformed token")
        void validateTokenWithMalformedToken() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            String invalidToken = "not.a.jwt";

            assertThatThrownBy(() -> jwtUtil.isAuthenticated(invalidToken, userDetails))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("isAuthenticated throws SignatureException for token with wrong signature")
        void validateTokenWithWrongSignature() throws Exception {
            // Create a token with a different signing key
            String wrongSignatureToken = Jwts.builder()
                    .setSubject(testUsername)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 600_000L))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("DifferentSecretKeyForTestingPurposes".getBytes()))
                    .compact();

            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

            assertThatThrownBy(() -> jwtUtil.isAuthenticated(wrongSignatureToken, userDetails))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("Null Input Handling")
    class NullInputTests {

        @Test
        @DisplayName("generateToken throws exception when UserDetails is null")
        void generateTokenWithNullUserDetails() {
            assertThatThrownBy(() -> jwtUtil.generateToken(null))
                    .isInstanceOf(NullPointerException.class);
        }


        @Test
        @DisplayName("isAuthenticated throws exception when UserDetails is null")
        void validateTokenWithNullUserDetails() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            String token = jwtUtil.generateToken(userDetails);

            assertThatThrownBy(() -> jwtUtil.isAuthenticated(token, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("isAuthenticated throws exception when token is null")
        void validateTokenWithNullToken() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

            assertThatThrownBy(() -> jwtUtil.isAuthenticated(null, userDetails))
                    .isInstanceOf(Exception.class); // NPE or IllegalArgumentException
        }

        @Test
        @DisplayName("extractUsername throws exception when token is null")
        void extractUsernameWithNullToken() {
            assertThatThrownBy(() -> jwtUtil.extractUsername(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("extractExpiration throws exception when token is null")
        void extractExpirationWithNullToken() {
            assertThatThrownBy(() -> jwtUtil.extractExpiration(null))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Additional Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("extractClaim with custom function works correctly")
        void extractClaimWithCustomFunction() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            String token = jwtUtil.generateToken(userDetails);

            // Test extracting issued-at claim
            Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
            assertThat(issuedAt).isNotNull();
            assertThat(issuedAt).isBeforeOrEqualTo(new Date());
        }

        @Test
        @DisplayName("generated token has correct issued-at time")
        void verifyIssuedAtClaim() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            
            long beforeGeneration = System.currentTimeMillis();
            String token = jwtUtil.generateToken(userDetails);
            long afterGeneration = System.currentTimeMillis();

            Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
            
            assertThat(issuedAt).isNotNull();
            // JWT timestamps are usually in seconds, so allow for some tolerance
            assertThat(issuedAt.getTime()).isBetween(beforeGeneration - 1000, afterGeneration + 1000);
        }

        @Test
        @DisplayName("isAuthenticated returns false when UserDetails username differs from token subject")
        void validateTokenUsernameMismatch() {
            var alice = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(alice.getUsername()).thenReturn("alice@example.com");
            
            var charlie = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(charlie.getUsername()).thenReturn("charlie@example.com");

            String token = jwtUtil.generateToken(alice);
            boolean valid = jwtUtil.isAuthenticated(token, charlie);
            
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("generateToken creates unique tokens for same user called multiple times")
        void generateTokenCreatesUniqueTokens() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);

            String token1 = jwtUtil.generateToken(userDetails);
            // Add sufficient delay to ensure different issued-at times (JWT uses seconds)
            try {
                Thread.sleep(1001); // Wait just over 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String token2 = jwtUtil.generateToken(userDetails);

            assertThat(token1).isNotEqualTo(token2);
            assertThat(jwtUtil.isAuthenticated(token1, userDetails)).isTrue();
            assertThat(jwtUtil.isAuthenticated(token2, userDetails)).isTrue();
        }

        @Test
        @DisplayName("extractClaim works with different claim types")
        void extractClaimWithDifferentTypes() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            String token = jwtUtil.generateToken(userDetails);

            // Test different return types
            String subject = jwtUtil.extractClaim(token, Claims::getSubject);
            Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);
            Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);

            assertThat(subject).isEqualTo(testUsername);
            assertThat(expiration).isNotNull();
            assertThat(issuedAt).isNotNull();
        }

        @Test
        @DisplayName("empty string token throws appropriate exception")
        void emptyStringToken() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            
            assertThatThrownBy(() -> jwtUtil.isAuthenticated("", userDetails))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("whitespace-only token throws appropriate exception")
        void whitespaceOnlyToken() {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            
            assertThatThrownBy(() -> jwtUtil.isAuthenticated("   ", userDetails))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token Structure and Claims Verification")
    class TokenStructureTests {

        @Test
        @DisplayName("generated token contains all required standard claims")
        void verifyAllStandardClaims() throws Exception {
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            
            String token = jwtUtil.generateToken(userDetails);
            Claims claims = getPrivateExtractAllClaims(token);
            
            // Verify all standard claims are present
            assertThat(claims.getSubject()).isEqualTo(testUsername);
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getIssuedAt()).isNotNull();
            
            // Verify expiration is after issued-at
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
            
            // Verify the time difference matches expected expiry time
            long timeDiff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertThat(timeDiff).isEqualTo(600_000L); // 10 minutes
        }

        @Test
        @DisplayName("token validation checks both username and expiration")
        void tokenValidationChecksUsernameAndExpiration() throws Exception {
            // Create a token that's valid now
            var userDetails = Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
            Mockito.when(userDetails.getUsername()).thenReturn(testUsername);
            String validToken = jwtUtil.generateToken(userDetails);
            
            // Valid token should pass
            assertThat(jwtUtil.isAuthenticated(validToken, userDetails)).isTrue();
            
            // Create an expired token with correct username
            String expiredToken = Jwts.builder()
                    .setSubject(testUsername)
                    .setIssuedAt(new Date(System.currentTimeMillis() - 2_000_000L))
                    .setExpiration(new Date(System.currentTimeMillis() - 1_000_000L))
                    .signWith(getPrivateSigningKey())
                    .compact();
                    
            // Expired token should throw exception (as verified in existing test)
            assertThatThrownBy(() -> jwtUtil.isAuthenticated(expiredToken, userDetails))
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        }
    }
}