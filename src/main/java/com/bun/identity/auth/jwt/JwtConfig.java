package com.bun.identity.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class JwtConfig {
    private final Resource privateKeyResource;
    private final Resource publicKeyResource;

    public JwtConfig(@Value("${jwt.private-key-path}") Resource privateKeyResource,
            @Value("${jwt.public-key-path}") Resource publicKeyResource) {
        this.privateKeyResource = privateKeyResource;
        this.publicKeyResource = publicKeyResource;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        RSAKey rsaKey = new RSAKey.Builder(getPublicKey())
                .privateKey(getPrivateKey())
                .build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(getPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    private RSAPrivateKey getPrivateKey() {
        try {
            byte[] keyBytes = decodePem(readPem(privateKeyResource), "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA private key configured for jwt.private-key-path", ex);
        }
    }

    private RSAPublicKey getPublicKey() {
        try {
            byte[] keyBytes = decodePem(readPem(publicKeyResource), "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA public key configured for jwt.public-key-path", ex);
        }
    }

    private static String readPem(Resource resource) throws Exception {
        if (!resource.exists()) {
            throw new IllegalArgumentException("JWT key file does not exist: " + resource);
        }
        try (var input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] decodePem(String pem, String keyType) {
        String beginMarker = "-----BEGIN " + keyType + "-----";
        String endMarker = "-----END " + keyType + "-----";
        String normalized = pem
                .replace("\\r", "")
                .replace("\\n", "\n")
                .replace("\r", "")
                .trim();

        if (!normalized.contains(beginMarker) || !normalized.contains(endMarker)) {
            throw new IllegalArgumentException("PEM must contain " + beginMarker + " and " + endMarker);
        }

        String body = normalized
                .replace(beginMarker, "")
                .replace(endMarker, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
