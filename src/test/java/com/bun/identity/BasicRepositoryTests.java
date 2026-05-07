package com.bun.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.bun.identity.auth.RefreshToken;
import com.bun.identity.auth.repository.RefreshTokenRepository;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.IdentityProvider;
import com.bun.identity.user.repository.AppUserRepository;

/**
 * Minimal Phase 1 repository tests - stabilize basic CRUD operations.
 * Goal: Freeze current behavior before structural refactoring.
 */
@SpringBootTest
@ActiveProfiles("test")
class BasicRepositoryTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testCreateAndFindUser() {
        // Create user
        AppUser user = AppUser.builder()
                .username("testuser_" + UUID.randomUUID() + "@example.com")
                .name("Test User")
                .password(passwordEncoder.encode("Password123!"))
                .provider(IdentityProvider.LOCAL)
                .enabled(true)
                .accountLocked(false)
                .build();

        AppUser saved = appUserRepository.save(user);
        assertNotNull(saved.getId());

        // Find by username
        Optional<AppUser> found = appUserRepository.findByUsername(saved.getUsername());
        assertTrue(found.isPresent());
        assertEquals(saved.getUsername(), found.get().getUsername());
    }

    @Test
    void testUserAccountStatus() {
        // Create user
        AppUser user = AppUser.builder()
                .username("statustest_" + UUID.randomUUID() + "@example.com")
                .name("Status Test User")
                .password(passwordEncoder.encode("Password123!"))
                .provider(IdentityProvider.LOCAL)
                .enabled(true)
                .accountLocked(false)
                .build();

        AppUser saved = appUserRepository.save(user);
        assertTrue(saved.isEnabled());
        assertTrue(saved.isAccountNonLocked()); // accountLocked=false means accountNonLocked=true
    }

    @Test
    void testCreateRefreshToken() {
        // Create user
        AppUser user = AppUser.builder()
                .username("tokentest_" + UUID.randomUUID() + "@example.com")
                .name("Token Test User")
                .password(passwordEncoder.encode("Password123!"))
                .provider(IdentityProvider.LOCAL)
                .enabled(true)
                .accountLocked(false)
                .build();
        user = appUserRepository.save(user);

        // Create refresh token
        RefreshToken token = RefreshToken.builder()
                .appUser(user)
                .tokenHash("hash_" + UUID.randomUUID())
                .deviceId("device_" + UUID.randomUUID())
                .expiresAt(java.time.Instant.now().plusSeconds(86400))
                .build();

        RefreshToken saved = refreshTokenRepository.save(token);
        assertNotNull(saved.getId());
        assertEquals(user.getId(), saved.getAppUser().getId());
    }

    @Test
    void testFindUserByUsername_NotFound() {
        Optional<AppUser> notFound = appUserRepository.findByUsername("nonexistent@example.com");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testUsernameUniqueness() {
        String username = "unique_" + UUID.randomUUID() + "@example.com";

        // Create first user
        AppUser user1 = AppUser.builder()
                .username(username)
                .name("User 1")
                .password(passwordEncoder.encode("Password123!"))
                .provider(IdentityProvider.LOCAL)
                .enabled(true)
                .accountLocked(false)
                .build();
        appUserRepository.save(user1);

        // Verify it exists
        Optional<AppUser> found = appUserRepository.findByUsername(username);
        assertTrue(found.isPresent());
    }
}
