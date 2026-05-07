package com.bun.identity.auth.oauth2;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.IdentityProvider;

@Component
public class GoogleOAuth2UserProfileExtractor implements OAuth2UserProfileExtractor {

    private static final String GOOGLE_REGISTRATION_ID = "google";

    @Override
    public boolean supports(String registrationId) {
        return GOOGLE_REGISTRATION_ID.equalsIgnoreCase(registrationId);
    }

    @Override
    public ExternalIdentityProfile extract(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub");
        Boolean emailVerified = oauth2User.getAttribute("email_verified");

        if (!StringUtils.hasText(email)) {
            throw IdentityException.validation("Google did not provide an email address");
        }
        if (!StringUtils.hasText(providerId)) {
            throw IdentityException.validation("Google did not provide a subject identifier");
        }

        return new ExternalIdentityProfile(
                IdentityProvider.GOOGLE,
                providerId,
                email,
                StringUtils.hasText(name) ? name : email,
                Boolean.TRUE.equals(emailVerified));
    }
}
