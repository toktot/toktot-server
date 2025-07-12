package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.UserAgreement;
import com.toktot.domain.user.UserProfile;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.domain.user.type.AuthProvider;
import com.toktot.web.dto.auth.response.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KakaoOAuth2Service {

    private final UserRepository userRepository;
    private final KakaoApiClient kakaoApiClient;

    public User processKakaoLogin(String authorizationCode, String clientIp, String userAgent) {
        logKakaoLoginStart(clientIp, userAgent);

        try {
            String accessToken = obtainAccessToken(authorizationCode, clientIp);
            KakaoUserInfoResponse userInfo = fetchKakaoUserInfo(accessToken, clientIp);
            User user = findOrCreateUser(userInfo, clientIp, userAgent);

            logKakaoLoginComplete(user, userInfo, clientIp);
            return user;

        } catch (ToktotException e) {
            logBusinessError(e, clientIp);
            throw e;
        } catch (Exception e) {
            logSystemError(e, clientIp);
            throw new ToktotException(ErrorCode.SERVER_ERROR, "카카오 로그인 처리 중 시스템 오류가 발생했습니다.");
        }
    }

    private void logKakaoLoginStart(String clientIp, String userAgent) {
        log.info("카카오 로그인 처리 시작 - clientIp: {}, userAgent: {}",
                clientIp, userAgent.substring(0, Math.min(50, userAgent.length())));
    }

    private String obtainAccessToken(String authorizationCode, String clientIp) {
        String accessToken = kakaoApiClient.getAccessToken(authorizationCode);
        log.debug("카카오 액세스 토큰 획득 성공 - clientIp: {}", clientIp);
        return accessToken;
    }

    private KakaoUserInfoResponse fetchKakaoUserInfo(String accessToken, String clientIp) {
        Map<String, Object> kakaoUserData = kakaoApiClient.getUserInfo(accessToken);
        KakaoUserInfoResponse userInfo = parseKakaoUserInfo(kakaoUserData);

        log.info("카카오 사용자 정보 조회 성공 - kakaoId: {}, nickname: {}, clientIp: {}",
                userInfo.id(), userInfo.nickname(), clientIp);

        return userInfo;
    }

    private User findOrCreateUser(KakaoUserInfoResponse userInfo, String clientIp, String userAgent) {
        Optional<User> existingUser = findExistingKakaoUser(userInfo.id());

        if (existingUser.isPresent()) {
            return handleExistingUser(existingUser.get(), userInfo.id(), clientIp);
        }

        return createNewKakaoUser(userInfo, clientIp, userAgent);
    }

    private void logKakaoLoginComplete(User user, KakaoUserInfoResponse userInfo, String clientIp) {
        boolean isNewUser = user.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(1));
        log.info("카카오 로그인 처리 완료 - userId: {}, kakaoId: {}, isNewUser: {}, clientIp: {}",
                user.getId(), userInfo.id(), isNewUser, clientIp);
    }

    private void logBusinessError(ToktotException e, String clientIp) {
        log.warn("카카오 로그인 비즈니스 로직 실패 - errorCode: {}, message: {}, clientIp: {}",
                e.getErrorCodeName(), e.getMessage(), clientIp);
    }

    private void logSystemError(Exception e, String clientIp) {
        log.error("카카오 로그인 처리 중 예상치 못한 오류 - clientIp: {}, error: {}",
                clientIp, e.getMessage(), e);
    }

    private KakaoUserInfoResponse parseKakaoUserInfo(Map<String, Object> responseBody) {
        try {
            String kakaoId = extractKakaoId(responseBody);
            String nickname = extractNickname(responseBody);
            String profileImageUrl = extractProfileImageUrl(responseBody);

            KakaoUserInfoResponse userInfo = KakaoUserInfoResponse.from(kakaoId, nickname, profileImageUrl);

            logNicknameInfo(userInfo);
            return userInfo;

        } catch (Exception e) {
            log.error("카카오 사용자 정보 파싱 실패 - responseBody: {}, error: {}",
                    responseBody, e.getMessage(), e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, "카카오 사용자 정보 형식이 올바르지 않습니다.");
        }
    }

    private String extractKakaoId(Map<String, Object> responseBody) {
        String kakaoId = String.valueOf(responseBody.get("id"));
        log.debug("카카오 사용자 정보 파싱 시작 - kakaoId: {}", kakaoId);
        return kakaoId;
    }

    private String extractNickname(Map<String, Object> responseBody) {
        String nickname = extractFromProperties(responseBody, "nickname");

        if (nickname == null) {
            nickname = extractFromKakaoAccount(responseBody, "nickname");
        }

        return nickname;
    }

    private String extractProfileImageUrl(Map<String, Object> responseBody) {
        String profileImageUrl = extractFromProperties(responseBody, "profile_image");

        if (profileImageUrl == null) {
            profileImageUrl = extractFromKakaoAccount(responseBody, "profile_image_url");
        }

        return profileImageUrl;
    }

    private String extractFromProperties(Map<String, Object> responseBody, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) responseBody.get("properties");

        if (properties != null) {
            String value = (String) properties.get(key);
            log.debug("카카오 properties에서 {} 추출: {}", key, value);
            return value;
        }

        log.debug("카카오 properties가 null입니다.");
        return null;
    }

    private String extractFromKakaoAccount(Map<String, Object> responseBody, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) responseBody.get("kakao_account");

        if (kakaoAccount != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            if (profile != null) {
                String value = (String) profile.get(key);
                log.debug("kakao_account.profile에서 {} 추출: {}", key, value);
                return value;
            }
        }

        return null;
    }

    private void logNicknameInfo(KakaoUserInfoResponse userInfo) {
        if (!userInfo.hasValidNickname()) {
            String generatedNickname = generateDefaultNickname(userInfo);
            log.info("카카오 닉네임 없음, 기본값 사용 - kakaoId: {}, generatedNickname: {}",
                    userInfo.id(), generatedNickname);
        }
    }

    private Optional<User> findExistingKakaoUser(String kakaoId) {
        log.debug("카카오 사용자 조회 시작 - kakaoId: {}", kakaoId);
        return userRepository.findByIdentifierAndAuthProvider(kakaoId, AuthProvider.KAKAO);
    }

    private User handleExistingUser(User user, String kakaoId, String clientIp) {
        log.info("기존 카카오 사용자 로그인 - userId: {}, kakaoId: {}, lastLogin: {}",
                user.getId(), kakaoId,
                user.getUserProfile() != null ? user.getUserProfile().getLastLoginAt() : "없음");

        updateLoginRecord(user, clientIp);
        return userRepository.save(user);
    }

    private void updateLoginRecord(User user, String clientIp) {
        if (user.getUserProfile() != null) {
            user.getUserProfile().recordSuccessfulLogin(clientIp);
            log.debug("로그인 기록 업데이트 완료 - userId: {}, clientIp: {}", user.getId(), clientIp);
        }
    }

    private User createNewKakaoUser(KakaoUserInfoResponse userInfo, String clientIp, String userAgent) {
        String nickname = determineNickname(userInfo);
        log.info("새 카카오 사용자 생성 시작 - kakaoId: {}, nickname: {}, clientIp: {}",
                userInfo.id(), nickname, clientIp);

        User newUser = buildKakaoUser(userInfo, nickname);
        User savedUser = userRepository.save(newUser);

        setupUserRelations(savedUser, clientIp, userAgent);
        User finalUser = userRepository.save(savedUser);

        log.info("새 카카오 사용자 생성 완료 - userId: {}, kakaoId: {}, nickname: {}, clientIp: {}",
                finalUser.getId(), userInfo.id(), nickname, clientIp);

        return finalUser;
    }

    private String determineNickname(KakaoUserInfoResponse userInfo) {
        return userInfo.hasValidNickname() ? userInfo.nickname() : generateDefaultNickname(userInfo);
    }

    private User buildKakaoUser(KakaoUserInfoResponse userInfo, String nickname) {
        User newUser = User.createKakaoUser(userInfo.id(), nickname, userInfo.profileImageUrl());
        log.debug("새 카카오 사용자 객체 생성 완료 - kakaoId: {}", userInfo.id());
        return newUser;
    }

    private void setupUserRelations(User savedUser, String clientIp, String userAgent) {
        UserProfile userProfile = createUserProfile(savedUser, clientIp);
        UserAgreement userAgreement = createUserAgreement(savedUser, clientIp, userAgent);

        assignUserRelations(savedUser, userProfile, userAgreement);
    }

    private UserProfile createUserProfile(User user, String clientIp) {
        UserProfile userProfile = UserProfile.createDefault(user);
        userProfile.recordSuccessfulLogin(clientIp);
        log.debug("사용자 프로필 생성 완료 - userId: {}", user.getId());
        return userProfile;
    }

    private UserAgreement createUserAgreement(User user, String clientIp, String userAgent) {
        UserAgreement userAgreement = UserAgreement.createWithFullAgreement(user, clientIp, userAgent);
        log.debug("사용자 약관 동의 생성 완료 - userId: {}", user.getId());
        return userAgreement;
    }

    private void assignUserRelations(User user, UserProfile userProfile, UserAgreement userAgreement) {
        user.assignUserProfile(userProfile);
        user.assignUserAgreement(userAgreement);
        log.debug("사용자 연관관계 설정 완료 - userId: {}", user.getId());
    }

    private String generateDefaultNickname(KakaoUserInfoResponse userInfo) {
        if (userInfo.hasValidNickname()) {
            return userInfo.nickname();
        }
        return "카카오사용자" + userInfo.id().substring(Math.max(0, userInfo.id().length() - 4));
    }
}
