package com.toktot.web.controller.user;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.service.*;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.auth.request.login.EmailLoginRequest;
import com.toktot.web.dto.auth.request.password.PasswordResetCompleteRequest;
import com.toktot.web.dto.auth.request.password.PasswordResetSendRequest;
import com.toktot.web.dto.auth.request.register.*;
import com.toktot.web.dto.auth.response.*;
import com.toktot.web.mapper.AuthResponseMapper;
import com.toktot.web.util.ClientInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class EmailAuthController {

    private final EmailAuthService emailAuthService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final AuthResponseMapper authResponseMapper;

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendVerificationEmail(
            @Valid @RequestBody EmailSendRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.info("이메일 인증 코드 발송 요청 - email: {}, clientIp: {}", request.email(), clientIp);

        try {
            emailAuthService.checkEmailDuplicate(request.email());
            emailVerificationService.sendVerificationCode(request.email());

            log.info("이메일 인증 코드 발송 성공 - email: {}, clientIp: {}", request.email(), clientIp);

            EmailSendResponse response = authResponseMapper.toEmailSendResponse(
                    request.email(), "인증 코드가 발송되었습니다."
            );

            return ResponseEntity.ok(
                    ApiResponse.success("인증 코드가 발송되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.warn("이메일 인증 코드 발송 실패 - email: {}, error: {}, clientIp: {}",
                    request.email(), e.getMessage(), clientIp);
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("이메일 인증 코드 발송 중 시스템 오류 - email: {}, clientIp: {}, error: {}",
                    request.email(), clientIp, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @Valid @RequestBody EmailVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.info("이메일 인증 코드 확인 요청 - email: {}, clientIp: {}", request.email(), clientIp);

        try {
            emailVerificationService.verifyCode(request.email(), request.verificationCode());

            log.info("이메일 인증 성공 - email: {}, clientIp: {}", request.email(), clientIp);

            return ResponseEntity.ok(
                    ApiResponse.success("이메일 인증이 완료되었습니다.", "verified")
            );

        } catch (ToktotException e) {
            log.warn("이메일 인증 실패 - email: {}, error: {}, clientIp: {}",
                    request.email(), e.getMessage(), clientIp);
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("이메일 인증 중 시스템 오류 - email: {}, clientIp: {}, error: {}",
                    request.email(), clientIp, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/nickname/check")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
            @Valid @RequestBody NicknameCheckRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.info("닉네임 중복 확인 요청 - nickname: {}, clientIp: {}", request.nickname(), clientIp);

        try {
            emailAuthService.checkNicknameDuplicate(request.nickname());

            log.info("닉네임 사용 가능 - nickname: {}, clientIp: {}", request.nickname(), clientIp);

            NicknameCheckResponse response = authResponseMapper.toNicknameAvailableResponse(request.nickname());

            return ResponseEntity.ok(
                    ApiResponse.success("사용 가능한 닉네임입니다.", response)
            );

        } catch (ToktotException e) {
            if (e.getErrorCode() == ErrorCode.DUPLICATE_USERNAME) {
                log.info("닉네임 중복 - nickname: {}, clientIp: {}", request.nickname(), clientIp);

                NicknameCheckResponse response = authResponseMapper.toNicknameUnavailableResponse(request.nickname());

                return ResponseEntity.ok(
                        ApiResponse.success("닉네임 중복 확인 완료", response)
                );
            }

            log.warn("닉네임 중복 확인 실패 - nickname: {}, error: {}, clientIp: {}",
                    request.nickname(), e.getMessage(), clientIp);
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("닉네임 중복 확인 중 시스템 오류 - nickname: {}, clientIp: {}, error: {}",
                    request.nickname(), clientIp, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> registerUser(
            @Valid @RequestBody RegisterCompleteRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);
        String userAgent = ClientInfoExtractor.getUserAgent(httpRequest);

        log.info("회원가입 요청 - email: {}, nickname: {}, clientIp: {}",
                request.email(), request.nickname(), clientIp);

        try {
            User user = emailAuthService.registerEmailUser(
                    request.email(),
                    request.password(),
                    request.nickname(),
                    clientIp,
                    userAgent
            );

            recordSuccessfulRegistration(user, clientIp, userAgent);

            log.info("회원가입 성공 - userId: {}, email: {}, nickname: {}, clientIp: {}",
                    user.getId(), request.email(), request.nickname(), clientIp);

            String successMessage = authResponseMapper.toRegistrationSuccessMessage(user);

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage)
            );

        } catch (ToktotException e) {
            log.warn("회원가입 실패 - email: {}, error: {}, clientIp: {}",
                    request.email(), e.getMessage(), clientIp);
            auditLogService.recordLoginFailure(request.email(), clientIp, userAgent, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("회원가입 중 시스템 오류 - email: {}, clientIp: {}, error: {}",
                    request.email(), clientIp, e.getMessage(), e);
            auditLogService.recordLoginFailure(request.email(), clientIp, userAgent, "시스템 오류");
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> emailLogin(
            @Valid @RequestBody EmailLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);
        String userAgent = ClientInfoExtractor.getUserAgent(httpRequest);

        log.info("이메일 로그인 요청 - email: {}, clientIp: {}", request.email(), clientIp);

        try {
            User user = emailAuthService.authenticateEmailUser(
                    request.email(),
                    request.password(),
                    clientIp
            );

            TokenResponse tokenResponse = generateAndSetTokens(user, httpResponse);
            recordSuccessfulLogin(user, clientIp, userAgent);

            log.info("이메일 로그인 성공 - userId: {}, email: {}, clientIp: {}",
                    user.getId(), request.email(), clientIp);

            String successMessage = authResponseMapper.toLoginSuccessMessage(user);
            TokenResponse publicTokenResponse = authResponseMapper.toPublicTokenResponse(tokenResponse);

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage, publicTokenResponse)
            );

        } catch (ToktotException e) {
            log.warn("이메일 로그인 실패 - email: {}, error: {}, clientIp: {}",
                    request.email(), e.getMessage(), clientIp);
            auditLogService.recordLoginFailure(request.email(), clientIp, userAgent, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("이메일 로그인 중 시스템 오류 - email: {}, clientIp: {}, error: {}",
                    request.email(), clientIp, e.getMessage(), e);
            auditLogService.recordLoginFailure(request.email(), clientIp, userAgent, "시스템 오류");
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/password/reset/send")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendPasswordResetCode(
            @Valid @RequestBody PasswordResetSendRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.info("비밀번호 재설정 코드 발송 요청 - email: {}, clientIp: {}", request.email(), clientIp);

        try {
            passwordResetService.sendPasswordResetCode(request.email());

            log.info("비밀번호 재설정 코드 발송 성공 - email: {}, clientIp: {}", request.email(), clientIp);

            EmailSendResponse response = authResponseMapper.toEmailSendResponse(
                    request.email(), "비밀번호 재설정 링크가 발송되었습니다."
            );

            return ResponseEntity.ok(
                    ApiResponse.success("비밀번호 재설정 링크가 발송되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.warn("비밀번호 재설정 코드 발송 실패 - email: {}, error: {}, clientIp: {}",
                    request.email(), e.getMessage(), clientIp);
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("비밀번호 재설정 코드 발송 중 시스템 오류 - email: {}, clientIp: {}, error: {}",
                    request.email(), clientIp, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/password/reset/complete")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody PasswordResetCompleteRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);
        log.info("비밀번호 재설정 요청 - email: {}, clientIp: {}", request.email(), clientIp);

        try {
            passwordResetService.resetPassword(
                    request.email(),
                    request.verificationCode(),
                    request.newPassword()
            );

            log.info("비밀번호 재설정 성공 - email: {}, clientIp: {}", request.email(), clientIp);

            return ResponseEntity.ok(
                    ApiResponse.success("비밀번호가 재설정되었습니다.", "completed")
            );

        } catch (ToktotException e) {
            log.warn("비밀번호 재설정 실패 - email: {}, error: {}, clientIp: {}",
                    request.email(), e.getMessage(), clientIp);
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("비밀번호 재설정 중 시스템 오류 - email: {}, clientIp: {}, error: {}",
                    request.email(), clientIp, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    private TokenResponse generateAndSetTokens(User user, HttpServletResponse httpResponse) {
        TokenResponse tokenResponse = authService.generateTokens(user);

        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(
                tokenResponse.refreshToken()
        );
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        log.debug("JWT 토큰 생성 및 쿠키 설정 완료 - userId: {}", user.getId());

        return tokenResponse;
    }

    private void recordSuccessfulRegistration(User user, String clientIp, String userAgent) {
        try {
            auditLogService.recordLoginSuccess(user, clientIp, userAgent, "EMAIL_REGISTER");
            log.debug("회원가입 감사로그 기록 완료 - userId: {}", user.getId());
        } catch (Exception e) {
            log.warn("회원가입 감사로그 기록 실패 - userId: {}, error: {}", user.getId(), e.getMessage());
        }
    }

    private void recordSuccessfulLogin(User user, String clientIp, String userAgent) {
        try {
            auditLogService.recordLoginSuccess(user, clientIp, userAgent, "EMAIL");
            log.debug("로그인 감사로그 기록 완료 - userId: {}", user.getId());
        } catch (Exception e) {
            log.warn("로그인 감사로그 기록 실패 - userId: {}, error: {}", user.getId(), e.getMessage());
        }
    }
}
