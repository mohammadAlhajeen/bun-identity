package com.bun.identity.user.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bun.identity.auth.DeviceIds;
import com.bun.identity.auth.oauth2.ExternalIdentityProfile;
import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.IdentityProvider;
import com.bun.identity.user.Role;
import com.bun.identity.user.dto.AppUserMapper;
import com.bun.identity.user.dto.AppUserRegisterReq;
import com.bun.identity.user.repository.AppUserRepository;
import com.bun.identity.user.security.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserOnboardingService {

    private final AppUserRepository appUserRepository;
    private final PasswordHasher passwordHasher;
    private final AppUserMapper appUserMapper;

    @Transactional
    public AppUser resolveOrCreateFromExternalIdentity(ExternalIdentityProfile profile) {
        return resolveOrCreateFromExternalIdentity(profile, null);
    }

    @Transactional
    public AppUser resolveOrCreateFromExternalIdentity(ExternalIdentityProfile profile, String deviceId) {
        if (!profile.emailVerified()) {
            throw IdentityException.unauthorized("OAuth2 provider email is not verified");
        }

        String normalizedEmail = normalizeEmail(profile.email());
        String normalizedProviderId = normalizeProviderId(profile.providerId());

        AppUser userByProviderId = appUserRepository.findByProviderId(normalizedProviderId).orElse(null);
        AppUser userByEmail = resolveUniqueUserByEmail(normalizedEmail);

        if (userByProviderId != null) {
            if (userByEmail != null && !userByProviderId.getId().equals(userByEmail.getId())) {
                throw IdentityException.conflict("OAuth2 provider is already linked to a different account");
            }

            userByProviderId.assertCanAuthenticate();
            return userByProviderId;
        }

        AppUser guestUser = findGuestUserByDeviceId(deviceId).orElse(null);
        if (guestUser != null) {
            if (userByEmail != null && !guestUser.getId().equals(userByEmail.getId())) {
                if (StringUtils.hasText(userByEmail.getProviderId())
                        && !normalizedProviderId.equals(userByEmail.getProviderId())) {
                    throw IdentityException.conflict("A different OAuth2 account is already linked to this email");
                }

                userByEmail.setProviderId(normalizedProviderId);
                userByEmail.setProvider(profile.provider());
                userByEmail.assertCanAuthenticate();
                return userByEmail;
            }

            return upgradeGuestToOAuthUser(guestUser, profile, normalizedEmail, normalizedProviderId);
        }

        if (userByEmail != null) {
            if (StringUtils.hasText(userByEmail.getProviderId())
                    && !normalizedProviderId.equals(userByEmail.getProviderId())) {
                throw IdentityException.conflict("A different OAuth2 account is already linked to this email");
            }

            userByEmail.setProviderId(normalizedProviderId);
            userByEmail.setProvider(profile.provider());
            userByEmail.assertCanAuthenticate();
            return userByEmail;
        }

        return createOAuth2User(profile, normalizedEmail, normalizedProviderId);
    }

    @Transactional
    public AppUser registerLocalUser(AppUserRegisterReq registerDTO, Set<Role> roles) {
        String normalizedDeviceId = DeviceIds.require(registerDTO.deviceId());
        String normalizedUsername = normalizeEmail(registerDTO.username());
        String hashedPassword = passwordHasher.hash(registerDTO.password());

        AppUser guestUser = findGuestUserByDeviceId(normalizedDeviceId).orElse(null);
        AppUser existingUser = resolveUniqueUserByEmail(normalizedUsername);
        if (existingUser != null && (guestUser == null || !existingUser.getId().equals(guestUser.getId()))) {
            throw IdentityException.conflict("Username already exists");
        }

        if (guestUser != null) {
            return upgradeGuestToLocalUser(guestUser, registerDTO, normalizedUsername, hashedPassword, roles);
        }

        AppUser appUser = appUserMapper.createAppUser(
                normalizedUsername,
                hashedPassword,
                registerDTO.name().trim(),
                StringUtils.hasText(registerDTO.phone()) ? registerDTO.phone().trim() : null);
        appUser.setProvider(IdentityProvider.LOCAL);
        appUser.setProviderId(null);
        appUser.setGuestDeviceId(null);
        appUser.setRoles(roles);

        AppUser savedAppUser = appUserRepository.save(appUser);
        log.info("Registered user {} with id {}", savedAppUser.getUsername(), savedAppUser.getId());
        return savedAppUser;
    }

    @Transactional
    public AppUser getOrCreateGuestUser(String deviceId) {
        String normalizedDeviceId = DeviceIds.require(deviceId);
        return findGuestUserByDeviceId(normalizedDeviceId)
                .orElseGet(() -> createGuestAppUser(normalizedDeviceId));
    }

    private AppUser createGuestAppUser(String normalizedDeviceId) {
        AppUser guestUser = new AppUser();
        guestUser.setUsername("guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "@guest.local");
        guestUser.setName("Guest");
        guestUser.setPassword(passwordHasher.hash(UUID.randomUUID().toString()));
        guestUser.setProvider(IdentityProvider.LOCAL);
        guestUser.setProviderId(null);
        guestUser.setGuestDeviceId(normalizedDeviceId);
        guestUser.setEnabled(true);
        guestUser.setAccountLocked(false);
        guestUser.setRoles(Set.of(Role.ROLE_GUEST));

        AppUser savedGuestUser = appUserRepository.save(guestUser);
        log.info("Created guest user {} for deviceId={}", savedGuestUser.getId(), normalizedDeviceId);
        return savedGuestUser;
    }

    private AppUser upgradeGuestToLocalUser(
            AppUser guestUser,
            AppUserRegisterReq registerDTO,
            String normalizedUsername,
            String hashedPassword,
            Set<Role> roles) {
        guestUser.assertGuestUpgradeCandidate();

        guestUser.setUsername(normalizedUsername);
        guestUser.setName(registerDTO.name().trim());
        guestUser.setPhone(StringUtils.hasText(registerDTO.phone()) ? registerDTO.phone().trim() : null);
        guestUser.setPassword(hashedPassword);
        guestUser.setProvider(IdentityProvider.LOCAL);
        guestUser.setProviderId(null);
        guestUser.setGuestDeviceId(null);
        guestUser.setRoles(roles);
        guestUser.setEnabled(true);
        guestUser.setAccountLocked(false);
        return appUserRepository.save(guestUser);
    }

    private AppUser createOAuth2User(ExternalIdentityProfile profile, String email, String providerId) {
        AppUser appUser = new AppUser();
        appUser.setUsername(email);
        appUser.setName(resolveDisplayName(profile.name(), email));
        appUser.setPassword(null);
        appUser.setRoles(Set.of(Role.ROLE_USER));
        appUser.setEnabled(true);
        appUser.setAccountLocked(false);
        appUser.setProvider(profile.provider());
        appUser.setProviderId(providerId);
        appUser.setGuestDeviceId(null);

        AppUser savedAppUser = appUserRepository.save(appUser);
        log.info("Registered {} user {}", profile.provider(), savedAppUser.getUsername());
        return savedAppUser;
    }

    private AppUser upgradeGuestToOAuthUser(
            AppUser guestUser,
            ExternalIdentityProfile profile,
            String email,
            String providerId) {
        guestUser.assertGuestUpgradeCandidate();

        AppUser emailOwner = resolveUniqueUserByEmail(email);
        if (emailOwner != null && !emailOwner.getId().equals(guestUser.getId())) {
            throw IdentityException.conflict("A different account is already linked to this email");
        }

        AppUser providerOwner = appUserRepository.findByProviderId(providerId).orElse(null);
        if (providerOwner != null && !providerOwner.getId().equals(guestUser.getId())) {
            throw IdentityException.conflict("OAuth2 provider is already linked to a different account");
        }

        guestUser.setUsername(email);
        guestUser.setName(resolveDisplayName(profile.name(), email));
        guestUser.setPassword(null);
        guestUser.setProvider(profile.provider());
        guestUser.setProviderId(providerId);
        guestUser.setGuestDeviceId(null);
        guestUser.setRoles(Set.of(Role.ROLE_USER));
        guestUser.setEnabled(true);
        guestUser.setAccountLocked(false);
        return appUserRepository.save(guestUser);
    }

    private Optional<AppUser> findGuestUserByDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return Optional.empty();
        }

        String normalizedDeviceId = DeviceIds.require(deviceId);
        return appUserRepository.findByGuestDeviceId(normalizedDeviceId)
                .filter(user -> user.hasRole(Role.ROLE_GUEST));
    }

    private AppUser resolveUniqueUserByEmail(String email) {
        var matches = appUserRepository.findAllByUsernameIgnoreCase(email);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            throw IdentityException.conflict("Multiple users share the same email address");
        }
        return matches.getFirst();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw IdentityException.validation("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeProviderId(String providerId) {
        if (!StringUtils.hasText(providerId)) {
            throw IdentityException.validation("OAuth2 provider did not return a provider identifier");
        }
        return providerId.trim();
    }

    private String resolveDisplayName(String name, String email) {
        return StringUtils.hasText(name) ? name.trim() : email;
    }
}
