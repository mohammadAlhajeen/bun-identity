package com.bun.identity.auth.oauth2;

import java.util.List;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.bun.identity.exception.IdentityException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2UserProfileExtractorRegistry {

    private final List<OAuth2UserProfileExtractor> extractors;

    public ExternalIdentityProfile extract(String registrationId, OAuth2User oauth2User) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(registrationId))
                .findFirst()
                .orElseThrow(() -> IdentityException.validation(
                        "Unsupported OAuth2 provider: " + registrationId))
                .extract(oauth2User);
    }
}
