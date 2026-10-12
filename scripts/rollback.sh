#!/bin/bash

set -e

PROJECT_DIR="${TOKTOT_PROJECT_DIR:-/home/ubuntu/toktot-server}"
SCRIPT_DIR="${TOKTOT_SCRIPT_DIR:-$PROJECT_DIR/scripts}"
DEPLOY_STATE_DIR="${TOKTOT_DEPLOY_STATE_DIR:-/home/ubuntu/.toktot-deploy}"
LAST_SUCCESS_TAG_FILE="$DEPLOY_STATE_DIR/last_success_tag"

# 로그 함수
log_info() {
    echo "🔄 [$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log_success() {
    echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log_error() {
    echo "❌ [$(date '+%Y-%m-%d %H:%M:%S')] $1" >&2
}

send_rollback_success_alert() {
    local rollback_tag="$1"

    if [ -n "$DISCORD_WEBHOOK_URL" ]; then
        curl -H "Content-Type: application/json" \
             -X POST \
             -d "{
               \"username\": \"TokTot Rollback Bot\",
               \"avatar_url\": \"https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png\",
               \"embeds\": [{
                 \"title\": \"🔄 TokTot Dev 서버 롤백 성공\",
                 \"description\": \"**✅ 자동 롤백 완료 - 서비스 정상 복구**\\n\\n**🔄 롤백된 버전**: \\\`${rollback_tag}\\\`\\n**⏰ 롤백 시간**: $(date)\\n\\n🟢 **서버 상태**: 정상 운영 (이전 버전)\",
                 \"color\": 65280,
                 \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\"
               }]
             }" \
             "$DISCORD_WEBHOOK_URL" || log_error "롤백 성공 알림 발송 실패"
    fi
}

main() {
    log_info "================================"
    log_info "자동 롤백 시작..."
    log_info "================================"

    cd "$PROJECT_DIR"

    # 1. 마지막 성공 버전 확인
    if [ ! -s "$LAST_SUCCESS_TAG_FILE" ]; then
        log_error "롤백할 이전 버전 정보가 없습니다: $LAST_SUCCESS_TAG_FILE"
        log_info "첫 배포이거나 아직 헬스체크를 통과한 배포가 없습니다."
        return 1
    fi

    if [ -z "$DOCKERHUB_USERNAME" ]; then
        log_error "DOCKERHUB_USERNAME이 설정되지 않아 롤백 이미지를 정할 수 없습니다."
        return 1
    fi

    local rollback_tag
    rollback_tag=$(tr -d '[:space:]' < "$LAST_SUCCESS_TAG_FILE")
    local rollback_image="$DOCKERHUB_USERNAME/toktot:$rollback_tag"
    log_info "롤백 대상 이미지: $rollback_image"

    # 2. 롤백 이미지 준비 (로컬에 없을 때만 다운로드)
    if ! docker image inspect "$rollback_image" >/dev/null 2>&1; then
        log_info "로컬에 롤백 이미지가 없어 다운로드합니다."
        docker pull "$rollback_image" || {
            log_error "롤백 이미지 다운로드 실패: $rollback_image"
            return 1
        }
    fi

    # 3. 현재 실패한 컨테이너 정리
    docker-compose down || log_info "정리할 컨테이너가 없습니다."

    # 4. 환경변수 설정 및 이전 버전으로 실행
    source "$SCRIPT_DIR/env-setup.sh"
    export APP_IMAGE_TAG="$rollback_tag"

    docker-compose up -d || {
        log_error "롤백 컨테이너 실행 실패"
        return 1
    }

    # 5. 롤백된 서버 헬스체크
    log_info "롤백된 서버 헬스체크 중..."

    if bash "$SCRIPT_DIR/health-check.sh" 60; then
        log_success "롤백 성공! 이전 버전으로 서비스 복구 완료"
        log_info "복구된 이미지: $rollback_image"
        send_rollback_success_alert "$rollback_tag"
        return 0
    fi

    log_error "롤백된 서버도 헬스체크 실패"
    return 1
}

# 메인 함수 실행
main "$@"
