package com.bun.identity.auth.oauth2;

import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserProfileExtractor {

    boolean supports(String registrationId);

    ExternalIdentityProfile extract(OAuth2User oauth2User);
}
