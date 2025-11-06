package com.toktot.domain.user.controller;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.dto.request.password.PasswordResetSendRequest;
import com.toktot.domain.user.dto.request.password.PasswordResetVerifyRequest;
import com.toktot.domain.user.dto.request.password.PasswordUpdateRequest;
import com.toktot.domain.user.dto.request.register.*;
import com.toktot.domain.user.dto.response.EmailCheckResponse;
import com.toktot.domain.user.dto.response.EmailSendResponse;
import com.toktot.domain.user.dto.response.NicknameCheckResponse;
import com.toktot.domain.user.dto.response.TokenResponse;
import com.toktot.domain.user.service.*;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.user.dto.request.login.EmailLoginRequest;
import com.toktot.domain.user.dto.response.password.PasswordResetVerifyResponse;
import com.toktot.domain.user.dto.response.password.PasswordUpdateResponse;
import com.toktot.domain.user.dto.mapper.AuthResponseMapper;
import com.toktot.common.util.ClientInfoExtractor;
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
@RequestMapping("/v1/auth")
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

        log.atInfo()
                .setMessage("Email verification code send request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            emailAuthService.checkEmailDuplicate(request.email());
            emailVerificationService.sendVerificationCode(request.email());

            log.atInfo()
                    .setMessage("Email verification code sent successfully")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            EmailSendResponse response = authResponseMapper.toEmailSendResponse(
                    request.email(), "인증 코드가 발송되었습니다."
            );

            return ResponseEntity.ok(
                    ApiResponse.success("인증 코드가 발송되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Email verification code send failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Email verification code send system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @Valid @RequestBody EmailVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.atInfo()
                .setMessage("Email verification request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            emailVerificationService.verifyCode(request.email(), request.verificationCode());

            log.atInfo()
                    .setMessage("Email verification successful")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success("이메일 인증이 완료되었습니다.", "verified")
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Email verification failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Email verification system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/nickname/check")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(
            @Valid @RequestBody NicknameCheckRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.atInfo()
                .setMessage("Nickname duplicate check request received")
                .addKeyValue("nickname", request.nickname())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            emailAuthService.checkNicknameDuplicate(request.nickname());

            log.atInfo()
                    .setMessage("Nickname available")
                    .addKeyValue("nickname", request.nickname())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            NicknameCheckResponse response = authResponseMapper.toNicknameAvailableResponse(request.nickname());

            return ResponseEntity.ok(
                    ApiResponse.success("사용 가능한 닉네임입니다.", response)
            );

        } catch (ToktotException e) {
            if (e.getErrorCode() == ErrorCode.DUPLICATE_USERNAME) {
                log.atInfo()
                        .setMessage("Nickname already taken")
                        .addKeyValue("nickname", request.nickname())
                        .addKeyValue("clientIp", clientIp)
                        .log();

                NicknameCheckResponse response = authResponseMapper.toNicknameUnavailableResponse(request.nickname());

                return ResponseEntity.ok(
                        ApiResponse.success("닉네임 중복 확인 완료", response)
                );
            }

            log.atWarn()
                    .setMessage("Nickname check failed")
                    .addKeyValue("nickname", request.nickname())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Nickname check system error")
                    .addKeyValue("nickname", request.nickname())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
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

        log.atInfo()
                .setMessage("User registration request received")
                .addKeyValue("email", request.email())
                .addKeyValue("nickname", request.nickname())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            User user = emailAuthService.registerEmailUser(
                    request.email(),
                    request.password(),
                    request.nickname(),
                    clientIp,
                    userAgent
            );

            recordSuccessfulRegistration(user, clientIp, userAgent);

            log.atInfo()
                    .setMessage("User registration successful")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("email", request.email())
                    .addKeyValue("nickname", request.nickname())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            String successMessage = authResponseMapper.toRegistrationSuccessMessage(user);

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("User registration failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            auditLogService.recordLoginFailure(request.email(), clientIp, userAgent, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("User registration system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
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

        log.atInfo()
                .setMessage("Email login request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            User user = emailAuthService.authenticateEmailUser(
                    request.email(),
                    request.password(),
                    clientIp
            );

            TokenResponse tokenResponse = generateAndSetTokens(user, httpResponse);
            recordSuccessfulLogin(user, clientIp, userAgent);

            log.atInfo()
                    .setMessage("Email login successful")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            String successMessage = authResponseMapper.toLoginSuccessMessage(user);
            TokenResponse publicTokenResponse = authResponseMapper.toPublicTokenResponse(tokenResponse);

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage, publicTokenResponse)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Email login failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            auditLogService.recordLoginFailure(request.email(), clientIp, userAgent, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Email login system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
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

        log.atInfo()
                .setMessage("Password reset code send request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            passwordResetService.sendPasswordResetCode(request.email());

            log.atInfo()
                    .setMessage("Password reset code sent successfully")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            EmailSendResponse response = authResponseMapper.toEmailSendResponse(
                    request.email(), "비밀번호 재설정 인증번호가 발송되었습니다."
            );

            return ResponseEntity.ok(
                    ApiResponse.success("비밀번호 재설정 인증번호가 발송되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Password reset code send failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Password reset code send system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<PasswordResetVerifyResponse>> verifyPasswordResetToken(
            @Valid @RequestBody PasswordResetVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.atInfo()
                .setMessage("Password reset token verification request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            passwordResetService.verifyResetToken(request.email(), request.verificationCode());

            log.atInfo()
                    .setMessage("Password reset token verification successful")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            PasswordResetVerifyResponse response = authResponseMapper.toPasswordResetVerifyResponse(request.email());

            return ResponseEntity.ok(
                    ApiResponse.success("인증이 완료되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Password reset token verification failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Password reset token verification system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/password/reset/update")
    public ResponseEntity<ApiResponse<PasswordUpdateResponse>> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.atInfo()
                .setMessage("Password update request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            passwordResetService.updatePassword(
                    request.email(),
                    request.newPassword()
            );

            log.atInfo()
                    .setMessage("Password update successful")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            PasswordUpdateResponse response = authResponseMapper.toPasswordUpdateResponse(request.email());

            return ResponseEntity.ok(
                    ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Password update failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Password update system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @PostMapping("/email/check")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmailDuplicate(
            @Valid @RequestBody EmailCheckRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);

        log.atInfo()
                .setMessage("Email duplicate check request received")
                .addKeyValue("email", request.email())
                .addKeyValue("clientIp", clientIp)
                .log();

        try {
            emailAuthService.checkEmailDuplicate(request.email());

            log.atInfo()
                    .setMessage("Email available")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .log();

            EmailCheckResponse response = authResponseMapper.toEmailAvailableResponse(request.email());

            return ResponseEntity.ok(
                    ApiResponse.success("사용 가능한 이메일입니다.", response)
            );

        } catch (ToktotException e) {
            if (e.getErrorCode() == ErrorCode.DUPLICATE_EMAIL) {
                log.atInfo()
                        .setMessage("Email already taken")
                        .addKeyValue("email", request.email())
                        .addKeyValue("clientIp", clientIp)
                        .log();

                EmailCheckResponse response = authResponseMapper.toEmailUnavailableResponse(request.email());

                return ResponseEntity.ok(
                        ApiResponse.success("이메일 중복 확인 완료", response)
                );
            }

            log.atWarn()
                    .setMessage("Email check failed")
                    .addKeyValue("email", request.email())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Email check system error")
                    .addKeyValue("email", request.email())
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    private TokenResponse generateAndSetTokens(User user, HttpServletResponse httpResponse) {
        TokenResponse tokenResponse = authService.generateTokens(user);

        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(
                tokenResponse.refreshToken()
        );
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        log.atDebug()
                .setMessage("JWT tokens generated and cookie set")
                .addKeyValue("userId", user.getId())
                .log();

        return tokenResponse;
    }

    private void recordSuccessfulRegistration(User user, String clientIp, String userAgent) {
        try {
            auditLogService.recordLoginSuccess(user, clientIp, userAgent, "EMAIL_REGISTER");

            log.atDebug()
                    .setMessage("Registration audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .log();
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("Failed to record registration audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("error", e.getMessage())
                    .log();
        }
    }

    private void recordSuccessfulLogin(User user, String clientIp, String userAgent) {
        try {
            auditLogService.recordLoginSuccess(user, clientIp, userAgent, "EMAIL");

            log.atDebug()
                    .setMessage("Login audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .log();
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("Failed to record login audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("error", e.getMessage())
                    .log();
        }
    }
}
