package com.toktot.domain.user.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditAction {
    // 인증 관련
    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAILED("로그인 실패"),
    LOGOUT("로그아웃"),

    // 회원 관리
    USER_REGISTER("회원가입"),
    USER_UPDATE("회원정보 수정"),
    USER_DELETE("회원탈퇴"),

    // 프로필 관리
    PROFILE_UPDATE("프로필 수정"),
    PROFILE_IMAGE_UPLOAD("프로필 이미지 업로드"),
    PROFILE_IMAGE_DELETE("프로필 이미지 삭제"),

    // 보안 관련
    PASSWORD_CHANGE("비밀번호 변경"),
    ACCOUNT_LOCK("계정 잠금"),
    ACCOUNT_UNLOCK("계정 잠금 해제"),

    // 동의 관리
    TERMS_AGREE("이용약관 동의"),
    TERMS_WITHDRAW("이용약관 철회"),
    PRIVACY_AGREE("개인정보 동의"),
    PRIVACY_WITHDRAW("개인정보 철회"),

    // 데이터 접근
    DATA_ACCESS("데이터 접근"),
    DATA_EXPORT("데이터 내보내기");

    private final String description;
}
