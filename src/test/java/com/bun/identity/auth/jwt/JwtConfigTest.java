package com.bun.identity.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.bun.identity.user.AppUser;
import com.bun.identity.user.Role;
import com.nimbusds.jwt.SignedJWT;

class JwtConfigTest {

    @Test
    void issuedTokenUsesRs256AndDecodesWithConfiguredPublicKey() throws Exception {
        JwtConfig jwtConfig = new JwtConfig(
                new ClassPathResource("jwt/jwt-private.pem"),
                new ClassPathResource("jwt/jwt-public.pem"));
        JwtEncoder encoder = jwtConfig.jwtEncoder();
        JwtDecoder decoder = jwtConfig.jwtDecoder();
        JwtTokenIssuer issuer = new JwtTokenIssuer(encoder, "identity-starter-test", 60);
        UUID userId = UUID.randomUUID();
        String deviceId = UUID.randomUUID().toString();
        AppUser user = AppUser.builder()
                .id(userId)
                .username("alice@example.com")
                .roles(Set.of(Role.ROLE_USER))
                .build();

        String token = issuer.issue(user, deviceId);
        Jwt decoded = decoder.decode(token);
        SignedJWT signedJwt = SignedJWT.parse(token);

        assertEquals("RS256", signedJwt.getHeader().getAlgorithm().getName());
        assertEquals("identity-starter-test", decoded.getClaimAsString("iss"));
        assertEquals(userId.toString(), decoded.getSubject());
        assertEquals(userId.toString(), decoded.getClaimAsString("uid"));
        assertEquals("alice@example.com", decoded.getClaimAsString("username"));
        assertEquals("ROLE_USER", decoded.getClaimAsString("scope"));
        assertEquals(deviceId, decoded.getClaimAsString("device_id"));
    }
}
