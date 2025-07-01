package com.toktot.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 인증 관련 에러 (AUTH)
    INVALID_PASSWORD("비밀번호가 틀렸습니다."),
    USER_NOT_FOUND("존재하지 않는 계정입니다."),
    TOKEN_EXPIRED("토큰이 만료되었습니다."),
    TOKEN_INVALID("유효하지 않은 토큰입니다."),
    LOGIN_REQUIRED("로그인이 필요합니다."),

    // 회원가입/사용자 관리 에러 (USER)
    DUPLICATE_EMAIL("이미 사용중인 이메일입니다."),
    DUPLICATE_USERNAME("이미 사용중인 아이디입니다."),
    INVALID_EMAIL_FORMAT("이메일 형식이 올바르지 않습니다."),
    WEAK_PASSWORD("비밀번호가 너무 간단합니다."),
    PASSWORD_MISMATCH("비밀번호가 일치하지 않습니다."),
    INVALID_USERNAME_FORMAT("아이디 형식이 올바르지 않습니다."),
    USERNAME_TOO_SHORT("아이디가 너무 짧습니다."),
    USERNAME_TOO_LONG("아이디가 너무 깁니다."),
    EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다."),

    // 권한 관련 에러 (PERMISSION)
    PERMISSION_DENIED("권한이 없습니다."),
    ACCESS_DENIED("접근이 거부되었습니다."),

    // 리소스 관련 에러 (RESOURCE)
    RESOURCE_NOT_FOUND("리소스를 찾을 수 없습니다."),
    POST_NOT_FOUND("게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다."),
    FILE_NOT_FOUND("파일을 찾을 수 없습니다."),

    // 입력값 검증 에러 (VALIDATION)
    INVALID_INPUT("입력값이 올바르지 않습니다."),
    MISSING_REQUIRED_FIELD("필수 입력값이 누락되었습니다."),
    INVALID_FORMAT("형식이 올바르지 않습니다."),
    VALUE_TOO_LONG("입력값이 너무 깁니다."),
    VALUE_TOO_SHORT("입력값이 너무 짧습니다."),
    INVALID_RANGE("유효하지 않은 범위입니다."),
    INVALID_DATE_FORMAT("날짜 형식이 올바르지 않습니다."),
    INVALID_TIME_FORMAT("시간 형식이 올바르지 않습니다."),
    INVALID_URL_FORMAT("URL 형식이 올바르지 않습니다."),
    INVALID_FILE_FORMAT("파일 형식이 올바르지 않습니다."),
    FILE_SIZE_EXCEEDED("파일 크기가 너무 큽니다."),

    // 비즈니스 로직 에러 (BUSINESS)
    OPERATION_NOT_ALLOWED("허용되지 않은 작업입니다."),
    ALREADY_PROCESSED("이미 처리된 요청입니다."),
    SERVICE_UNAVAILABLE("서비스를 사용할 수 없습니다."),
    FEATURE_DISABLED("비활성화된 기능입니다."),
    MAINTENANCE_MODE("점검 중입니다."),

    // 외부 서비스 연동 에러 (EXTERNAL)
    EXTERNAL_SERVICE_ERROR("외부 서비스 오류입니다."),
    EMAIL_SEND_FAILED("이메일 전송에 실패했습니다."),
    FILE_UPLOAD_FAILED("파일 업로드에 실패했습니다."),

    // 시스템 에러 (SYSTEM)
    SERVER_ERROR("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요."),
    DATABASE_ERROR("데이터베이스 오류가 발생했습니다."),
    NETWORK_ERROR("네트워크 오류가 발생했습니다."),
    TIMEOUT_ERROR("요청 시간이 초과되었습니다."),
    UNKNOWN_ERROR("알 수 없는 오류가 발생했습니다.");

    private final String message;

    public String getCode() {
        return this.name();
    }

    public boolean isAuthError() {
        return this.name().startsWith("INVALID_PASSWORD") ||
                this.name().startsWith("USER_NOT_FOUND") ||
                this.name().startsWith("ACCOUNT_") ||
                this.name().startsWith("TOKEN_") ||
                this.name().equals("UNAUTHORIZED") ||
                this.name().contains("LOGIN");
    }

    public boolean isValidationError() {
        return this.name().startsWith("INVALID_") ||
                this.name().startsWith("MISSING_") ||
                this.name().startsWith("VALUE_") ||
                this.name().contains("FORMAT") ||
                this.name().contains("SIZE");
    }

    public boolean isPermissionError() {
        return this.name().contains("PERMISSION") ||
                this.name().contains("ACCESS") ||
                this.name().contains("REQUIRED");
    }

    public boolean isSystemError() {
        return this.name().contains("ERROR") ||
                this.name().equals("SERVER_ERROR") ||
                this.name().equals("DATABASE_ERROR") ||
                this.name().equals("NETWORK_ERROR");
    }
}
