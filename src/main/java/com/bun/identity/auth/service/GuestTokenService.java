package com.bun.identity.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.service.UserOnboardingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestTokenService {

        private static final String PFX_LOGIN = "[LOGIN] ";

        private final UserOnboardingService userOnboardingService;
        private final AccessTokenIssuer accessTokenIssuer;

        @Transactional
        public TokenResponse issueGuestToken(String deviceId) {
                AppUser guestUser = userOnboardingService.getOrCreateGuestUser(deviceId);
                var access = accessTokenIssuer.issue(guestUser, deviceId);

                log.info(PFX_LOGIN + "Guest access token issued for userId={}, deviceId={}",
                                guestUser.getId(), deviceId);

                return new TokenResponse(access.token(), "", access.expiresInSeconds());
        }
}
