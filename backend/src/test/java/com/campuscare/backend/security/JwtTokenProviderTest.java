package com.campuscare.backend.security;

import com.campuscare.backend.entity.Profile;
import com.campuscare.backend.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";
    private static final String TEST_ISSUER = "https://xyz.supabase.co/auth/v1";
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
    }

    @Test
    void testGenerateAndValidateTokenWithConfiguredSecretAndIssuer() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        UUID id = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(id);
        profile.setEmail("rahul.s@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        String token = jwtTokenProvider.generateToken(profile);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(id, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("rahul.s@srmist.edu.in", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void testFailSecurelyWhenSecretIsMissing() {
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setEmail("student@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        // Generation must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> jwtTokenProvider.generateToken(profile));

        // Validation of an arbitrary token must fail securely
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3OC0xMjM0LTEyMzQtMTIzNC0xMjM0NTY3ODEyMzQiLCJleHAiOjI1MDAwMDAwMDB9.fakesig";
        assertFalse(jwtTokenProvider.validateToken(fakeToken));
    }

    @Test
    void testFailSecurelyWhenNoIssuerOrSupabaseUrlIsConfigured() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        // Neither configuredIssuer nor supabaseUrl is set

        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setEmail("student@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        // Token generation must fail securely
        assertThrows(IllegalStateException.class, () -> jwtTokenProvider.generateToken(profile));

        // Token validation must fail securely even if issuer contains 'supabase'
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"supabase\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        String token = signToken(header, payload, TEST_SECRET);
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testRejectLooseWordSupabaseFallbackInIssuer() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer("https://actual-project.supabase.co/auth/v1");

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        // Token with loose "supabase" or "attacker-supabase.com" must be rejected
        String payloadLoose = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"supabase\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        assertFalse(jwtTokenProvider.validateToken(signToken(header, payloadLoose, TEST_SECRET)));

        String payloadAttacker = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"https://attacker-supabase.com/auth/v1\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        assertFalse(jwtTokenProvider.validateToken(signToken(header, payloadAttacker, TEST_SECRET)));
    }

    @Test
    void testDerivedIssuerFromSupabaseUrl() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setSupabaseUrl("https://myproject.supabase.co");

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String validPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"https://myproject.supabase.co/auth/v1\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        assertTrue(jwtTokenProvider.validateToken(signToken(header, validPayload, TEST_SECRET)));

        String invalidPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"https://otherproject.supabase.co/auth/v1\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        assertFalse(jwtTokenProvider.validateToken(signToken(header, invalidPayload, TEST_SECRET)));
    }

    @Test
    void testRejectTamperedSignature() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        UUID id = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(id);
        profile.setEmail("test@srmist.edu.in");
        profile.setRole(UserRole.STUDENT);

        String validToken = jwtTokenProvider.generateToken(profile);
        String[] parts = validToken.split("\\.");

        char alteredChar = (parts[2].charAt(0) == 'A') ? 'B' : 'A';
        String tamperedSig = alteredChar + parts[2].substring(1);
        String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSig;

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    void testRejectExpiredToken() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        long pastExp = Instant.now().getEpochSecond() - 3600; // 1 hour ago
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"email\":\"test@srmist.edu.in\",\"iss\":\"%s\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), TEST_ISSUER, pastExp).getBytes(StandardCharsets.UTF_8)
        );

        String token = signToken(header, payload, TEST_SECRET);
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testRejectMissingOrInvalidClaims() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        // 1. Missing exp claim
        String payloadWithoutExp = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"%s\",\"aud\":\"authenticated\"}", UUID.randomUUID(), TEST_ISSUER).getBytes(StandardCharsets.UTF_8)
        );
        assertFalse(jwtTokenProvider.validateToken(signToken(header, payloadWithoutExp, TEST_SECRET)));

        // 2. Missing sub claim
        String payloadWithoutSub = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"iss\":\"%s\",\"aud\":\"authenticated\",\"exp\":%d}", TEST_ISSUER, Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        assertFalse(jwtTokenProvider.validateToken(signToken(header, payloadWithoutSub, TEST_SECRET)));

        // 3. Sub is not a valid UUID
        String payloadInvalidSub = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"not-a-uuid\",\"iss\":\"%s\",\"aud\":\"authenticated\",\"exp\":%d}", TEST_ISSUER, Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        assertFalse(jwtTokenProvider.validateToken(signToken(header, payloadInvalidSub, TEST_SECRET)));
    }

    @Test
    void testRejectInvalidJwtIssuer() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadWithBadIssuer = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"https://malicious.org/auth/v1\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );

        String badToken = signToken(header, payloadWithBadIssuer, TEST_SECRET);
        assertFalse(jwtTokenProvider.validateToken(badToken));
    }

    @Test
    void testRejectMissingJwtIssuer() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadWithoutIssuer = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );

        String token = signToken(header, payloadWithoutIssuer, TEST_SECRET);
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testRejectInvalidJwtAudience() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);
        jwtTokenProvider.setConfiguredAudience("authenticated");

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadWithBadAud = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"%s\",\"aud\":\"anon\",\"exp\":%d}",
                        UUID.randomUUID(), TEST_ISSUER, Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );

        String badToken = signToken(header, payloadWithBadAud, TEST_SECRET);
        assertFalse(jwtTokenProvider.validateToken(badToken));
    }

    @Test
    void testRejectMissingJwtAudience() {
        jwtTokenProvider.setConfiguredSecret(TEST_SECRET);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadWithoutAud = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"%s\",\"exp\":%d}",
                        UUID.randomUUID(), TEST_ISSUER, Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );

        String token = signToken(header, payloadWithoutAud, TEST_SECRET);
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testRejectUnsignedNoneAlgorithm() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"iss\":\"%s\",\"aud\":\"authenticated\",\"exp\":%d}",
                        UUID.randomUUID(), TEST_ISSUER, Instant.now().getEpochSecond() + 3600).getBytes(StandardCharsets.UTF_8)
        );
        String unsignedToken = header + "." + payload + ".";

        assertFalse(jwtTokenProvider.validateToken(unsignedToken));
    }

    @Test
    void testAsymmetricRs256Validation() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        String pemPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----";

        jwtTokenProvider.setConfiguredPublicKey(pemPublicKey);
        jwtTokenProvider.setConfiguredIssuer(TEST_ISSUER);

        UUID userId = UUID.randomUUID();
        long exp = Instant.now().getEpochSecond() + 3600;
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("{\"sub\":\"%s\",\"email\":\"prof@srmist.edu.in\",\"iss\":\"%s\",\"aud\":\"authenticated\",\"exp\":%d}",
                        userId, TEST_ISSUER, exp).getBytes(StandardCharsets.UTF_8)
        );

        String content = header + "." + payload;
        Signature rsaSigner = Signature.getInstance("SHA256withRSA");
        rsaSigner.initSign(pair.getPrivate());
        rsaSigner.update(content.getBytes(StandardCharsets.UTF_8));
        String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaSigner.sign());

        String validRs256Token = content + "." + sig;

        assertTrue(jwtTokenProvider.validateToken(validRs256Token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(validRs256Token));
        assertEquals("prof@srmist.edu.in", jwtTokenProvider.getEmailFromToken(validRs256Token));

        KeyPair otherPair = generator.generateKeyPair();
        Signature wrongSigner = Signature.getInstance("SHA256withRSA");
        wrongSigner.initSign(otherPair.getPrivate());
        wrongSigner.update(content.getBytes(StandardCharsets.UTF_8));
        String wrongSig = Base64.getUrlEncoder().withoutPadding().encodeToString(wrongSigner.sign());

        assertFalse(jwtTokenProvider.validateToken(content + "." + wrongSig));
    }

    @Test
    void testMalformedTokens() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken("not.enough.parts.extra"));
        assertFalse(jwtTokenProvider.validateToken("single-string"));
        assertFalse(jwtTokenProvider.validateToken("badheader.badpayload.badsig"));
    }

    private String signToken(String header, String payload, String secret) {
        try {
            String content = header + "." + payload;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
            return content + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
