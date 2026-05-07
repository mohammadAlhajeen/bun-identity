package com.bun.identity.user.service;

import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bun.identity.auth.service.LogoutService;
import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.IdentityProvider;
import com.bun.identity.user.dto.AppUserInfoDto;
import com.bun.identity.user.dto.AppUserMapper;
import com.bun.identity.user.dto.ChangePasswordReq;
import com.bun.identity.user.dto.UpdateAppUserDto;
import com.bun.identity.user.repository.AppUserRepository;
import com.bun.identity.user.security.PasswordHasher;
import com.bun.identity.user.security.SecurityUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordHasher passwordHasher;
    private final AppUserMapper appUserMapper;
    private final LogoutService logoutService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findByUsername(username)
                .map(SecurityUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public void changePassword(UUID appUserId, ChangePasswordReq changePasswordReq) {
        AppUser user = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getProvider() != IdentityProvider.LOCAL || !StringUtils.hasText(user.getPassword())) {
            throw IdentityException.forbidden("Password change is unavailable for externally authenticated accounts");
        }

        if (!passwordHasher.matches(changePasswordReq.oldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }

        if (passwordHasher.matches(changePasswordReq.newPassword(), user.getPassword())) {
            throw IdentityException.validation("New password must be different from the old one");
        }

        user.setPassword(passwordHasher.hash(changePasswordReq.newPassword()));
        log.info("Password changed for user {}", user.getUsername());
        logoutService.logoutAllDevices(appUserId);
        save(user);
    }

    @Transactional
    public AppUser save(@NonNull AppUser appUser) {
        return appUserRepository.save(appUser);
    }

    @Transactional(readOnly = true)
    public AppUserInfoDto getProfile(UUID id) {
        AppUser appUser = getUser(id);
        return appUserMapper.toUserInfo(appUser);
    }

    @Transactional(readOnly = true)
    public AppUser getUser(@NonNull UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> IdentityException.notFound("User not found"));
    }

    @Transactional
    public AppUserInfoDto updateProfile(UUID appUserId, UpdateAppUserDto appUserDto) {
        var updatedAppUser = updateProfileEntity(appUserId, appUserDto);
        return appUserMapper.toUserInfo(updatedAppUser);
    }

    private AppUser updateProfileEntity(UUID appUserId, UpdateAppUserDto appUserDto) {
        var appUser = appUserRepository.findById(appUserId)
                .orElseThrow(() -> IdentityException.notFound("User not found"));
        appUserMapper.updateDtoToAppUser(appUser, appUserDto);
        log.info("Updated profile for user {} with id {}", appUser.getUsername(), appUser.getId());
        return appUser;
    }
}
