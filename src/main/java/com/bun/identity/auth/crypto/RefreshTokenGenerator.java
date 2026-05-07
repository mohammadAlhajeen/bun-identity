package com.bun.identity.auth.crypto;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generator for opaque refresh tokens (256-bit random)
 */
public final class RefreshTokenGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int TOKEN_BYTES = 32; // 256-bit

    private RefreshTokenGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generate a new cryptographically secure opaque token
     * 
     * @return Base64 URL-encoded token without padding
     */
    public static String newOpaqueToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
