package com.bun.identity.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bun.identity.auth.oauth2.ExternalIdentityProfile;
import com.bun.identity.exception.ApiException;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.IdentityProvider;
import com.bun.identity.user.Role;
import com.bun.identity.user.dto.AppUserMapper;
import com.bun.identity.user.dto.AppUserRegisterReq;
import com.bun.identity.user.repository.AppUserRepository;
import com.bun.identity.user.security.PasswordHasher;

@ExtendWith(MockitoExtension.class)
class UserOnboardingServiceTest {

        @Mock
        private AppUserRepository appUserRepository;
        @Mock
        private PasswordHasher passwordHasher;
        @Mock
        private AppUserMapper appUserMapper;

        @InjectMocks
        private UserOnboardingService service;

        @Test
        void registerLocalUserCreatesRoleUser() {
                String deviceId = UUID.randomUUID().toString();
                AppUserRegisterReq request = new AppUserRegisterReq(
                                "A@Example.com",
                                "Password1",
                                "Alice",
                                "+970599123456",
                                deviceId);
                AppUser mapped = new AppUser();
                mapped.setUsername("a@example.com");
                mapped.setPassword("hashed");
                mapped.setName("Alice");
                mapped.setPhone("+970599123456");

                when(passwordHasher.hash("Password1")).thenReturn("hashed");
                when(appUserRepository.findByGuestDeviceId(deviceId)).thenReturn(Optional.empty());
                when(appUserRepository.findAllByUsernameIgnoreCase("a@example.com")).thenReturn(List.of());
                when(appUserMapper.createAppUser("a@example.com", "hashed", "Alice", "+970599123456"))
                                .thenReturn(mapped);
                when(appUserRepository.save(mapped)).thenReturn(mapped);

                AppUser result = service.registerLocalUser(request, Set.of(Role.ROLE_USER));

                assertSame(mapped, result);
                assertEquals("a@example.com", result.getUsername());
                assertEquals("hashed", result.getPassword());
                assertTrue(result.hasRole(Role.ROLE_USER));
                assertNull(result.getGuestDeviceId());
        }

        @Test
        void getOrCreateGuestUserReusesExistingGuestByDeviceId() {
                String deviceId = UUID.randomUUID().toString();
                AppUser guest = AppUser.builder()
                                .guestDeviceId(deviceId)
                                .roles(Set.of(Role.ROLE_GUEST))
                                .build();

                when(appUserRepository.findByGuestDeviceId(deviceId)).thenReturn(Optional.of(guest));

                AppUser result = service.getOrCreateGuestUser(deviceId);

                assertSame(guest, result);
                verify(appUserRepository, never()).save(any());
        }

        @Test
        void registerLocalUserUpgradesGuestUser() {
                String deviceId = UUID.randomUUID().toString();
                AppUser guest = AppUser.builder()
                                .id(UUID.randomUUID())
                                .username("guest@example.local")
                                .guestDeviceId(deviceId)
                                .roles(Set.of(Role.ROLE_GUEST))
                                .build();
                AppUserRegisterReq request = new AppUserRegisterReq(
                                "alice@example.com",
                                "Password1",
                                "Alice",
                                null,
                                deviceId);

                when(passwordHasher.hash("Password1")).thenReturn("hashed");
                when(appUserRepository.findByGuestDeviceId(deviceId)).thenReturn(Optional.of(guest));
                when(appUserRepository.findAllByUsernameIgnoreCase("alice@example.com")).thenReturn(List.of());
                when(appUserRepository.save(guest)).thenReturn(guest);

                AppUser result = service.registerLocalUser(request, Set.of(Role.ROLE_USER));

                assertSame(guest, result);
                assertEquals("alice@example.com", result.getUsername());
                assertEquals("hashed", result.getPassword());
                assertTrue(result.hasRole(Role.ROLE_USER));
                assertNull(result.getGuestDeviceId());
        }

        @Test
        void resolveOrCreateFromExternalIdentityCreatesOAuthUser() {
                ExternalIdentityProfile profile = new ExternalIdentityProfile(
                                IdentityProvider.GOOGLE,
                                "google-sub",
                                "A@Example.com",
                                "Alice",
                                true);

                when(appUserRepository.findByProviderId("google-sub")).thenReturn(Optional.empty());
                when(appUserRepository.findAllByUsernameIgnoreCase("a@example.com")).thenReturn(List.of());
                when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

                AppUser result = service.resolveOrCreateFromExternalIdentity(profile);

                assertEquals("a@example.com", result.getUsername());
                assertEquals(IdentityProvider.GOOGLE, result.getProvider());
                assertEquals("google-sub", result.getProviderId());
                assertTrue(result.hasRole(Role.ROLE_USER));
                assertNull(result.getPassword());
        }

        @Test
        void resolveOrCreateFromExternalIdentityUpgradesGuestUser() {
                String deviceId = UUID.randomUUID().toString();
                AppUser guest = AppUser.builder()
                                .id(UUID.randomUUID())
                                .username("guest@example.local")
                                .guestDeviceId(deviceId)
                                .roles(Set.of(Role.ROLE_GUEST))
                                .build();
                ExternalIdentityProfile profile = new ExternalIdentityProfile(
                                IdentityProvider.GOOGLE,
                                "google-sub",
                                "alice@example.com",
                                "Alice",
                                true);

                when(appUserRepository.findByProviderId("google-sub")).thenReturn(Optional.empty());
                when(appUserRepository.findAllByUsernameIgnoreCase("alice@example.com")).thenReturn(List.of());
                when(appUserRepository.findByGuestDeviceId(deviceId)).thenReturn(Optional.of(guest));
                when(appUserRepository.save(guest)).thenReturn(guest);

                AppUser result = service.resolveOrCreateFromExternalIdentity(profile, deviceId);

                assertSame(guest, result);
                assertEquals("alice@example.com", result.getUsername());
                assertEquals(IdentityProvider.GOOGLE, result.getProvider());
                assertTrue(result.hasRole(Role.ROLE_USER));
                assertNull(result.getGuestDeviceId());
        }

        @Test
        void registerLocalUserRejectsDuplicateEmail() {
                String deviceId = UUID.randomUUID().toString();
                AppUser existing = AppUser.builder().id(UUID.randomUUID()).username("alice@example.com").build();
                AppUserRegisterReq request = new AppUserRegisterReq(
                                "alice@example.com",
                                "Password1",
                                "Alice",
                                null,
                                deviceId);

                when(passwordHasher.hash("Password1")).thenReturn("hashed");
                when(appUserRepository.findByGuestDeviceId(deviceId)).thenReturn(Optional.empty());
                when(appUserRepository.findAllByUsernameIgnoreCase("alice@example.com")).thenReturn(List.of(existing));

                ApiException ex = assertThrows(ApiException.class,
                                () -> service.registerLocalUser(request, Set.of(Role.ROLE_USER)));

                assertEquals("IDENTITY_CONFLICT", ex.getCode());
        }

        @Test
        void resolveOrCreateFromExternalIdentityRejectsProviderEmailConflict() {
                AppUser providerOwner = AppUser.builder().id(UUID.randomUUID()).providerId("google-sub").build();
                AppUser emailOwner = AppUser.builder().id(UUID.randomUUID()).username("alice@example.com").build();
                ExternalIdentityProfile profile = new ExternalIdentityProfile(
                                IdentityProvider.GOOGLE,
                                "google-sub",
                                "alice@example.com",
                                "Alice",
                                true);

                when(appUserRepository.findByProviderId("google-sub")).thenReturn(Optional.of(providerOwner));
                when(appUserRepository.findAllByUsernameIgnoreCase("alice@example.com"))
                                .thenReturn(List.of(emailOwner));

                ApiException ex = assertThrows(ApiException.class,
                                () -> service.resolveOrCreateFromExternalIdentity(profile));

                assertEquals("IDENTITY_CONFLICT", ex.getCode());
        }
}
