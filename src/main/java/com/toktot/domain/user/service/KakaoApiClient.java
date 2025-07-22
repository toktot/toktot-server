package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class KakaoApiClient {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public KakaoApiClient(@Qualifier("kakaoWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public String getAccessToken(String authorizationCode) {
        log.debug("카카오 액세스 토큰 요청 시작 - authCode: {}...",
                authorizationCode.substring(0, Math.min(8, authorizationCode.length())));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoClientId);
        formData.add("client_secret", kakaoClientSecret);
        formData.add("redirect_uri", kakaoRedirectUri);
        formData.add("code", authorizationCode);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient
                    .post()
                    .uri(KAKAO_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null || !response.containsKey("access_token")) {
                log.error("카카오 토큰 응답에 access_token이 없음 - response: {}, authCode: {}..., clientId: {}",
                        response, authorizationCode.substring(0, Math.min(8, authorizationCode.length())), kakaoClientId);
                throw new ToktotException(ErrorCode.KAKAO_LOGIN_FAILED);
            }

            String accessToken = (String) response.get("access_token");
            log.debug("카카오 액세스 토큰 획득 성공");
            return accessToken;

        } catch (WebClientResponseException e) {
            log.error("카카오 액세스 토큰 요청 실패 - status: {}, statusText: {}, body: {}, headers: {}, authCode: {}...",
                    e.getStatusCode(),
                    e.getStatusText(),
                    e.getResponseBodyAsString(),
                    e.getHeaders(),
                    authorizationCode.substring(0, Math.min(8, authorizationCode.length())));

            // 토큰 만료/무효한 경우
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("카카오 인증 코드 만료/무효 - authCode: {}..., responseBody: {}",
                        authorizationCode.substring(0, Math.min(8, authorizationCode.length())),
                        e.getResponseBodyAsString());
                throw new ToktotException(ErrorCode.KAKAO_TOKEN_INVALID);
            }

            // 그 외 모든 경우
            log.error("카카오 토큰 요청 기타 오류 - statusCode: {}, clientId: {}, redirectUri: {}",
                    e.getStatusCode(), kakaoClientId, kakaoRedirectUri);
            throw new ToktotException(ErrorCode.KAKAO_LOGIN_FAILED);

        } catch (Exception e) {
            log.error("카카오 액세스 토큰 획득 중 예상치 못한 오류 - authCode: {}..., clientId: {}, error: {}",
                    authorizationCode.substring(0, Math.min(8, authorizationCode.length())),
                    kakaoClientId, e.getMessage(), e);
            throw new ToktotException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String accessToken) {
        log.debug("카카오 사용자 정보 요청 시작");

        try {
            Map<String, Object> response = webClient
                    .get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                log.error("카카오 사용자 정보 응답이 null - url: {}, accessToken: {}...",
                        KAKAO_USER_INFO_URL, accessToken.substring(0, Math.min(10, accessToken.length())));
                throw new ToktotException(ErrorCode.KAKAO_LOGIN_FAILED);
            }

            log.debug("카카오 사용자 정보 조회 성공 - kakaoId: {}", response.get("id"));
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 사용자 정보 요청 실패 - status: {}, statusText: {}, body: {}, headers: {}, accessToken: {}...",
                    e.getStatusCode(),
                    e.getStatusText(),
                    e.getResponseBodyAsString(),
                    e.getHeaders(),
                    accessToken.substring(0, Math.min(10, accessToken.length())));

            // 토큰 무효한 경우
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("카카오 액세스 토큰 무효/만료 - accessToken: {}..., responseBody: {}",
                        accessToken.substring(0, Math.min(10, accessToken.length())),
                        e.getResponseBodyAsString());
                throw new ToktotException(ErrorCode.KAKAO_TOKEN_INVALID);
            }

            // 그 외 모든 경우
            log.error("카카오 사용자 정보 요청 기타 오류 - statusCode: {}, url: {}, accessToken: {}...",
                    e.getStatusCode(), KAKAO_USER_INFO_URL, accessToken.substring(0, Math.min(10, accessToken.length())));
            throw new ToktotException(ErrorCode.KAKAO_LOGIN_FAILED);

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 중 예상치 못한 오류 - url: {}, accessToken: {}..., error: {}",
                    KAKAO_USER_INFO_URL, accessToken.substring(0, Math.min(10, accessToken.length())), e.getMessage(), e);
            throw new ToktotException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }
}
