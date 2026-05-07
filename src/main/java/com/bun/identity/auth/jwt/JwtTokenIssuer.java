package com.bun.identity.auth.jwt;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.service.UserIdentityService;

@Component
public class JwtTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationMinutes;

    public JwtTokenIssuer(JwtEncoder jwtEncoder,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.expiration}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(AppUser auth, String deviceId) {
        if (auth.getId() == null) {
            throw IdentityException.internal("Cannot issue token for user without an ID", null);
        }

        String scope = auth.getRoles() == null ? ""
                : auth.getRoles().stream().map(Enum::name).collect(Collectors.joining(" "));
        String userId = auth.getId().toString();
        var now = Instant.now();
        var expiration = now.plusSeconds(expirationMinutes * 60L);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).type("JWT").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiration)
                .subject(userId)
                .claim(UserIdentityService.USER_ID_CLAIM, userId)
                .claim("username", auth.getUsername())
                .claim("scope", scope)
                .claim("device_id", deviceId)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return expirationMinutes * 60L;
    }
}
