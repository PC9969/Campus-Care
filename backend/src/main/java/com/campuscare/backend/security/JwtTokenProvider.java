package com.campuscare.backend.security;

import com.campuscare.backend.entity.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    private static final long TOKEN_VALIDITY_SECONDS = 86400 * 7; // 7 days
    private static final long CLOCK_SKEW_SECONDS = 60; // 60s tolerance

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final Map<String, PublicKey> cachedPublicKeys = new ConcurrentHashMap<>();
    private volatile Instant jwksLastFetched = null;

    @Value("${supabase.jwt-secret:}")
    private String configuredSecret;

    @Value("${supabase.jwt-public-key:}")
    private String configuredPublicKey;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:}")
    private String supabaseAnonKey;

    @Value("${supabase.jwt-issuer:}")
    private String configuredIssuer;

    @Value("${supabase.jwt-audience:authenticated}")
    private String configuredAudience;

    public JwtTokenProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void setConfiguredSecret(String configuredSecret) {
        this.configuredSecret = configuredSecret;
    }

    public void setConfiguredPublicKey(String configuredPublicKey) {
        this.configuredPublicKey = configuredPublicKey;
    }

    public void setSupabaseUrl(String supabaseUrl) {
        this.supabaseUrl = supabaseUrl;
    }

    public void setSupabaseAnonKey(String supabaseAnonKey) {
        this.supabaseAnonKey = supabaseAnonKey;
    }

    public void setConfiguredIssuer(String configuredIssuer) {
        this.configuredIssuer = configuredIssuer;
    }

    public void setConfiguredAudience(String configuredAudience) {
        this.configuredAudience = configuredAudience;
    }

    /**
     * Generates a signed HS256 JWT. Fails securely if no signing secret is configured.
     */
    public String generateToken(Profile profile) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT signing secret is not configured; cannot generate signed token");
        }

        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + TOKEN_VALIDITY_SECONDS;

            String issuer = (configuredIssuer != null && !configuredIssuer.isBlank())
                    ? configuredIssuer.trim()
                    : (supabaseUrl != null && !supabaseUrl.isBlank() ? supabaseUrl.replaceAll("/+$", "") + "/auth/v1" : null);
            if (issuer == null) {
                throw new IllegalStateException("Supabase issuer or Supabase URL must be configured to generate a token");
            }
            String audience = (configuredAudience != null && !configuredAudience.isBlank())
                    ? configuredAudience.trim()
                    : "authenticated";

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = String.format(
                    "{\"sub\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"iss\":\"%s\",\"aud\":\"%s\",\"iat\":%d,\"exp\":%d}",
                    profile.getId(),
                    profile.getEmail(),
                    profile.getRole().name(),
                    issuer,
                    audience,
                    now,
                    exp
            );

            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = encodedHeader + "." + encodedPayload;
            String signature = signHmac(content, configuredSecret.trim());

            return content + "." + signature;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Could not generate JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Cryptographically validates the JWT token against configured keys/secrets.
     * Rejects unsigned, expired, unverified, or claim-invalid tokens.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            JsonNode header = parseJson(parts[0]);
            if (header == null || !header.has("alg")) {
                return false;
            }

            String alg = header.get("alg").asText();
            if (alg == null || alg.equalsIgnoreCase("none") || alg.isBlank()) {
                return false;
            }

            String kid = header.has("kid") ? header.get("kid").asText() : null;
            byte[] signedContent = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

            // 1. Verify cryptographic signature based on algorithm
            boolean signatureValid = switch (alg.toUpperCase()) {
                case "HS256" -> verifyHmacSha256(signedContent, signatureBytes);
                case "RS256" -> verifyRsaSha256(signedContent, signatureBytes, kid);
                case "ES256" -> verifyEcdsaSha256(signedContent, signatureBytes, kid);
                default -> false; // Reject unknown algorithms
            };

            if (!signatureValid) {
                return false;
            }

            // 2. Validate token claims
            JsonNode payload = parseJson(parts[1]);
            if (payload == null) {
                return false;
            }

            // Verify exp claim
            if (!payload.has("exp")) {
                return false;
            }
            long exp = payload.get("exp").asLong();
            long now = Instant.now().getEpochSecond();
            if (now > exp + CLOCK_SKEW_SECONDS) {
                return false; // Expired
            }

            // Verify nbf claim if present
            if (payload.has("nbf")) {
                long nbf = payload.get("nbf").asLong();
                if (now + CLOCK_SKEW_SECONDS < nbf) {
                    return false;
                }
            }

            // Verify sub claim
            if (!payload.has("sub")) {
                return false;
            }
            String sub = payload.get("sub").asText();
            if (sub == null || sub.isBlank()) {
                return false;
            }
            try {
                UUID.fromString(sub.trim());
            } catch (IllegalArgumentException e) {
                return false;
            }

            // Verify email claim if present
            if (payload.has("email") && payload.get("email").asText().isBlank()) {
                return false;
            }

            // Verify aud claim (expected Supabase audience, default "authenticated")
            if (!payload.has("aud")) {
                return false;
            }
            String expectedAudience = (configuredAudience != null && !configuredAudience.isBlank())
                    ? configuredAudience.trim()
                    : "authenticated";

            JsonNode audNode = payload.get("aud");
            boolean audMatches = false;
            if (audNode.isTextual()) {
                audMatches = expectedAudience.equals(audNode.asText().trim());
            } else if (audNode.isArray()) {
                for (JsonNode item : audNode) {
                    if (expectedAudience.equals(item.asText().trim())) {
                        audMatches = true;
                        break;
                    }
                }
            }
            if (!audMatches) {
                return false;
            }

            // Verify iss claim (expected Supabase issuer)
            if (!payload.has("iss")) {
                return false;
            }
            String tokenIssuer = payload.get("iss").asText();
            if (tokenIssuer == null || tokenIssuer.isBlank()) {
                return false;
            }

            boolean issMatches;
            if (configuredIssuer != null && !configuredIssuer.isBlank()) {
                issMatches = configuredIssuer.trim().equals(tokenIssuer.trim());
            } else if (supabaseUrl != null && !supabaseUrl.isBlank()) {
                String cleanBase = supabaseUrl.replaceAll("/+$", "");
                issMatches = tokenIssuer.equals(cleanBase + "/auth/v1") || tokenIssuer.equals(cleanBase);
            } else {
                // If neither configuredIssuer nor supabaseUrl is configured, fail securely
                return false;
            }
            if (!issMatches) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            JsonNode payload = parseJson(parts[1]);
            if (payload != null && payload.has("sub")) {
                return UUID.fromString(payload.get("sub").asText().trim());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            JsonNode payload = parseJson(parts[1]);
            if (payload != null && payload.has("email")) {
                return payload.get("email").asText().trim();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean verifyHmacSha256(byte[] signedContent, byte[] signatureBytes) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            return false; // Fail securely when secret is missing
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    configuredSecret.trim().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] expected = mac.doFinal(signedContent);
            return MessageDigest.isEqual(signatureBytes, expected);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyRsaSha256(byte[] signedContent, byte[] signatureBytes, String kid) {
        PublicKey key = resolvePublicKey(kid, "RSA");
        if (key == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(signedContent);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyEcdsaSha256(byte[] signedContent, byte[] signatureBytes, String kid) {
        PublicKey key = resolvePublicKey(kid, "EC");
        if (key == null) {
            return false;
        }
        try {
            // First attempt with native P1363 IEEE format (JWT standard raw R||S 64 bytes)
            try {
                Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
                signature.initVerify(key);
                signature.update(signedContent);
                if (signature.verify(signatureBytes)) {
                    return true;
                }
            } catch (NoSuchAlgorithmException ignored) {}

            // Fallback to DER-encoded ECDSA signature verification
            byte[] derSignature = p1363ToDer(signatureBytes);
            if (derSignature != null) {
                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initVerify(key);
                signature.update(signedContent);
                return signature.verify(derSignature);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private PublicKey resolvePublicKey(String kid, String expectedKty) {
        // 1. Try explicitly configured public key string (PEM format)
        if (configuredPublicKey != null && !configuredPublicKey.isBlank()) {
            PublicKey key = parsePemPublicKey(configuredPublicKey.trim(), expectedKty);
            if (key != null) return key;
        }

        // 2. Check cached JWKS
        if (kid != null && cachedPublicKeys.containsKey(kid)) {
            return cachedPublicKeys.get(kid);
        }

        // 3. Attempt JWKS fetch if Supabase URL is available
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            if (cachedPublicKeys.isEmpty() || (kid != null && !cachedPublicKeys.containsKey(kid))
                    || jwksLastFetched == null || jwksLastFetched.isBefore(Instant.now().minusSeconds(3600))) {
                refreshJwks();
            }
        }

        if (kid != null && cachedPublicKeys.containsKey(kid)) {
            return cachedPublicKeys.get(kid);
        }

        // Return first matching key if kid was omitted
        if (kid == null && !cachedPublicKeys.isEmpty()) {
            for (PublicKey pk : cachedPublicKeys.values()) {
                if (expectedKty.equalsIgnoreCase(pk.getAlgorithm())) {
                    return pk;
                }
            }
        }

        return null;
    }

    private synchronized void refreshJwks() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return;
        try {
            String jwksUrl = supabaseUrl.replaceAll("/+$", "") + "/auth/v1/.well-known/jwks.json";
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .GET();

            if (supabaseAnonKey != null && !supabaseAnonKey.isBlank()) {
                reqBuilder.header("apikey", supabaseAnonKey.trim());
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("keys") && root.get("keys").isArray()) {
                    for (JsonNode keyNode : root.get("keys")) {
                        String kty = keyNode.has("kty") ? keyNode.get("kty").asText() : "";
                        String keyId = keyNode.has("kid") ? keyNode.get("kid").asText() : null;
                        if (keyId == null) continue;

                        if ("RSA".equalsIgnoreCase(kty) && keyNode.has("n") && keyNode.has("e")) {
                            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("n").asText()));
                            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("e").asText()));
                            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
                            PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(spec);
                            cachedPublicKeys.put(keyId, pk);
                        } else if ("EC".equalsIgnoreCase(kty) && keyNode.has("x") && keyNode.has("y")) {
                            BigInteger x = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("x").asText()));
                            BigInteger y = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("y").asText()));
                            ECPoint point = new ECPoint(x, y);

                            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
                            params.init(new ECGenParameterSpec("secp256r1"));
                            ECParameterSpec ecParams = params.getParameterSpec(ECParameterSpec.class);
                            PublicKey pk = KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, ecParams));
                            cachedPublicKeys.put(keyId, pk);
                        }
                    }
                    jwksLastFetched = Instant.now();
                }
            }
        } catch (Exception ignored) {
            // Log/fail securely if JWKS is unreachable
        }
    }

    private PublicKey parsePemPublicKey(String pem, String expectedKty) {
        try {
            String clean = pem
                    .replaceAll("-----BEGIN [A-Z0-9 ]+-----", "")
                    .replaceAll("-----END [A-Z0-9 ]+-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(clean);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);

            String[] algs = "EC".equalsIgnoreCase(expectedKty) ? new String[]{"EC", "RSA"} : new String[]{"RSA", "EC"};
            for (String alg : algs) {
                try {
                    return KeyFactory.getInstance(alg).generatePublic(spec);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] p1363ToDer(byte[] p1363) {
        if (p1363.length != 64) {
            return null;
        }
        try {
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(p1363, 0, r, 0, 32);
            System.arraycopy(p1363, 32, s, 0, 32);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(0x30); // SEQUENCE

            ByteArrayOutputStream content = new ByteArrayOutputStream();
            writeDerInteger(content, r);
            writeDerInteger(content, s);

            byte[] body = content.toByteArray();
            bos.write(body.length);
            bos.write(body);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeDerInteger(ByteArrayOutputStream bos, byte[] val) throws Exception {
        bos.write(0x02); // INTEGER
        int start = 0;
        while (start < val.length - 1 && val[start] == 0) {
            start++;
        }
        int len = val.length - start;
        if ((val[start] & 0x80) != 0) {
            bos.write(len + 1);
            bos.write(0x00);
        } else {
            bos.write(len);
        }
        bos.write(val, start, len);
    }

    private JsonNode parseJson(String base64UrlString) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64UrlString);
            return objectMapper.readTree(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private String signHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
