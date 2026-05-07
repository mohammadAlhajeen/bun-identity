package com.bun.identity.auth.oauth2;

import com.bun.identity.user.IdentityProvider;

public record ExternalIdentityProfile(
        IdentityProvider provider,
        String providerId,
        String email,
        String name,
        boolean emailVerified) {
}
