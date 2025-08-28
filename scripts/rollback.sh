#!/bin/bash

set -e

PROJECT_DIR="/home/ubuntu/toktot-server"
SCRIPT_DIR="/home/ubuntu/toktot-server/scripts"

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

main() {
    log_info "================================"
    log_info "자동 롤백 시작..."
    log_info "================================"

    cd "$PROJECT_DIR"

    # 이전 이미지 정보 확인
    if [ -f "/tmp/last_working_image.txt" ]; then
        PREVIOUS_IMAGE=$(cat /tmp/last_working_image.txt)
        log_info "이전 이미지로 롤백 중: $PREVIOUS_IMAGE"

        # 현재 실패한 컨테이너 정리
        docker-compose down || log_info "컨테이너 정리 완료"

        # 이전 이미지로 롤백
        if [[ "$PREVIOUS_IMAGE" == *":"* ]]; then
            OLD_TAG="${PREVIOUS_IMAGE##*:}"
            log_info "롤백할 태그: $OLD_TAG"

            # 환경변수 설정 및 이전 버전으로 실행
            source "$SCRIPT_DIR/env-setup.sh"

            # docker-compose.yml에서 이미지 태그 임시 변경
            sed -i.bak "s|image: .*toktot:.*|image: $PREVIOUS_IMAGE|g" docker-compose.yml

            docker-compose up -d

            # 원본 docker-compose.yml 복원
            mv docker-compose.yml.bak docker-compose.yml 2>/dev/null || true

            # 롤백된 서버 헬스체크
            log_info "롤백된 서버 헬스체크 중..."

            if bash "$SCRIPT_DIR/health-check.sh" 60; then
                log_success "롤백 성공! 이전 버전으로 서비스 복구 완료"
                log_info "복구된 이미지: $PREVIOUS_IMAGE"

                # 롤백 성공 알림
                if [ -n "$DISCORD_WEBHOOK_URL" ]; then
                    curl -H "Content-Type: application/json" \
                         -X POST \
                         -d "{
                           \"username\": \"TokTot Rollback Bot\",
                           \"avatar_url\": \"https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png\",
                           \"embeds\": [{
                             \"title\": \"🔄 TokTot Dev 서버 롤백 성공\",
                             \"description\": \"**✅ 자동 롤백 완료 - 서비스 정상 복구**\\n\\n**🔄 롤백된 버전**: \\\`${OLD_TAG}\\\`\\n**⏰ 롤백 시간**: $(date)\\n\\n🟢 **서버 상태**: 정상 운영 (이전 버전)\",
                             \"color\": 65280,
                             \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\"
                           }]
                         }" \
                         "$DISCORD_WEBHOOK_URL" || log_error "롤백 성공 알림 발송 실패"
                fi

                return 0
            else
                log_error "롤백된 서버도 헬스체크 실패"
                return 1
            fi
        else
            log_error "이전 이미지 태그를 파싱할 수 없습니다: $PREVIOUS_IMAGE"
            return 1
        fi
    else
        log_error "롤백할 이전 버전 정보가 없습니다."
        log_info "첫 배포이거나 백업 정보가 손상되었습니다."

        # 기본 이미지로 복구 시도
        log_info "최신 stable 이미지로 복구 시도 중..."
        source "$SCRIPT_DIR/env-setup.sh"

        # stable 태그가 있다면 사용, 없다면 dev 태그 사용
        docker pull "$DOCKERHUB_USERNAME/toktot:stable" 2>/dev/null || docker pull "$DOCKERHUB_USERNAME/toktot:dev"
        docker-compose up -d

        if bash "$SCRIPT_DIR/health-check.sh" 60; then
            log_success "기본 이미지로 복구 성공"
            return 0
        else
            log_error "기본 이미지 복구도 실패"
            return 1
        fi
    fi
}

# 메인 함수 실행
main "$@"
