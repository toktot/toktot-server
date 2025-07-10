package com.toktot.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class OAuth2Config {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri:}")
    private String kakaoRedirectUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        log.info("OAuth2 ClientRegistrationRepository 구성 시작");

        if (!StringUtils.hasText(kakaoClientId)) {
            log.warn("카카오 OAuth2 클라이언트 ID가 설정되지 않았습니다. 카카오 로그인을 사용할 수 없습니다.");
            return new InMemoryClientRegistrationRepository();
        }

        ClientRegistration kakaoRegistration = kakaoClientRegistration();
        log.info("카카오 OAuth2 클라이언트 등록 완료");

        return new InMemoryClientRegistrationRepository(kakaoRegistration);
    }

    private ClientRegistration kakaoClientRegistration() {
        return ClientRegistration.withRegistrationId("kakao")
                .clientId(kakaoClientId)
                .clientSecret(kakaoClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(kakaoRedirectUri)
                .scope("profile_nickname", "profile_image", "account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }
}
