#!/bin/bash

set -eE  # 에러 발생 시 즉시 종료 (함수 안의 에러도 ERR 트랩으로 전달)

# 스크립트 디렉토리
PROJECT_DIR="${TOKTOT_PROJECT_DIR:-/home/ubuntu/toktot-server}"
SCRIPT_DIR="${TOKTOT_SCRIPT_DIR:-$PROJECT_DIR/scripts}"
DEPLOY_STATE_DIR="${TOKTOT_DEPLOY_STATE_DIR:-/home/ubuntu/.toktot-deploy}"
LAST_SUCCESS_TAG_FILE="$DEPLOY_STATE_DIR/last_success_tag"

# ✅ 배포 브랜치 설정 (환경변수로 받거나 기본값 main 사용)
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"

CONTAINERS_REPLACED=false

# 로그 함수
log_info() {
    echo "ℹ️  [$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log_success() {
    echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log_error() {
    echo "❌ [$(date '+%Y-%m-%d %H:%M:%S')] $1" >&2
}

log_warning() {
    echo "⚠️  [$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 에러 핸들러
handle_error() {
    local exit_code=$?
    log_error "배포 스크립트 실행 중 오류 발생 (종료 코드: $exit_code)"
    fail_deploy "$exit_code"
}

fail_deploy() {
    local exit_code="${1:-1}"
    trap - ERR

    if [ "$CONTAINERS_REPLACED" = true ]; then
        run_rollback
    else
        log_warning "기존 컨테이너를 내리기 전에 실패해 롤백 없이 종료합니다 (기존 서비스 유지)"
    fi

    exit "$exit_code"
}

run_rollback() {
    if [ ! -f "$SCRIPT_DIR/rollback.sh" ]; then
        log_error "rollback.sh를 찾을 수 없습니다: $SCRIPT_DIR/rollback.sh"
        send_emergency_alert "배포 실패 + rollback.sh 없음"
        return 0
    fi

    log_info "자동 롤백 시작..."
    if ! bash "$SCRIPT_DIR/rollback.sh"; then
        log_error "롤백도 실패했습니다. 긴급 대응 필요!"
        send_emergency_alert "배포 실패 + 롤백 실패"
    fi
}

# 긴급 알림 함수
send_emergency_alert() {
    local failure_reason="$1"

    if [ -n "$DISCORD_WEBHOOK_URL" ]; then
        curl -H "Content-Type: application/json" \
             -X POST \
             -d "{
               \"username\": \"TokTot EMERGENCY Bot\",
               \"avatar_url\": \"https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png\",
               \"embeds\": [{
                 \"title\": \"🚨 CRITICAL: TokTot 서버 완전 중단\",
                 \"description\": \"**❌ 배포 실패 + 롤백 실패 → 서비스 완전 중단**\\n\\n**실패 원인**: $failure_reason\",
                 \"color\": 16711680,
                 \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\"
               }]
             }" \
             "$DISCORD_WEBHOOK_URL" || log_error "긴급 알림 발송 실패"
    fi
}

# 에러 트랩 설정
trap handle_error ERR

# 메인 배포 함수
main() {
    log_info "🚀 TokTot 서버 배포 시작 (브랜치: $DEPLOY_BRANCH)..."

    # 0. 필수 환경변수 사전 검증
    log_info "필수 환경변수 검증 중..."
    if [ -z "$DOCKERHUB_USERNAME" ]; then
        log_error "DOCKERHUB_USERNAME 환경변수가 설정되지 않았습니다!"
        fail_deploy 1
    fi

    if [ -z "$COMMIT_SHA" ]; then
        log_error "COMMIT_SHA 환경변수가 설정되지 않았습니다! 배포할 이미지 태그를 정할 수 없습니다."
        fail_deploy 1
    fi

    APP_IMAGE_TAG="${DEPLOY_BRANCH}-${COMMIT_SHA}"
    APP_IMAGE="$DOCKERHUB_USERNAME/toktot:$APP_IMAGE_TAG"
    export APP_IMAGE_TAG
    log_info "배포 이미지: $APP_IMAGE"

    # 1. 기본 검증
    if [ ! -d "$PROJECT_DIR" ]; then
        log_error "프로젝트 디렉토리를 찾을 수 없습니다: $PROJECT_DIR"
        fail_deploy 1
    fi

    cd "$PROJECT_DIR"

    # 2. Git 저장소 확인
    if [ ! -d ".git" ]; then
        log_error "Git 저장소를 찾을 수 없습니다!"
        fail_deploy 1
    fi

    # 3. Git 업데이트 (✅ main 브랜치로 변경)
    log_info "Git 저장소 업데이트 중 (브랜치: $DEPLOY_BRANCH)..."

    # 로컬 변경사항 안전하게 백업
    log_info "로컬 변경사항 백업 중..."
    if ! git diff --quiet || ! git diff --cached --quiet; then
        log_warning "로컬 변경사항 감지됨. 백업 중..."
        git stash push -m "deploy-backup-$(date +%Y%m%d%H%M%S)" || {
            log_warning "stash 실패했지만 계속 진행합니다."
        }
    fi

    # Git fetch 및 강제 리셋
    git fetch origin || {
        log_error "Git fetch 실패!"
        fail_deploy 1
    }

    # ✅ 로컬 브랜치를 origin/main으로 강제 리셋
    git checkout $DEPLOY_BRANCH 2>/dev/null || git checkout -b $DEPLOY_BRANCH
    git reset --hard origin/$DEPLOY_BRANCH || {
        log_error "Git reset 실패!"
        fail_deploy 1
    }

    log_success "Git 저장소 업데이트 완료 (현재 브랜치: $(git rev-parse --abbrev-ref HEAD))"

    # 4. 필요한 디렉토리 생성
    log_info "필요한 디렉토리 생성 중..."
    mkdir -p nginx/conf.d certbot/www certbot/conf

    # 5. 롤백 기준 버전 확인
    if [ -s "$LAST_SUCCESS_TAG_FILE" ]; then
        log_info "롤백 기준 버전 (마지막 성공 배포): $(cat "$LAST_SUCCESS_TAG_FILE")"
    else
        log_warning "마지막 성공 배포 기록이 없습니다. 이번 배포가 실패하면 롤백할 버전이 없습니다."
    fi

    # 6. Docker 이미지 pull 및 검증 (✅ 커밋 SHA 태그 사용)
    log_info "배포 이미지 다운로드 중: $APP_IMAGE"

    docker pull "$APP_IMAGE" || {
        log_error "Docker 이미지 pull 실패!"
        fail_deploy 1
    }

    # Docker 이미지 무결성 검증
    log_info "Docker 이미지 무결성 검증 중..."
    if ! docker image inspect "$APP_IMAGE" >/dev/null 2>&1; then
        log_error "Docker 이미지가 손상되었거나 존재하지 않습니다!"
        fail_deploy 1
    fi
    log_success "Docker 이미지 검증 완료"

    # 7. 환경변수 설정
    log_info "환경변수 설정 중..."
    source "$SCRIPT_DIR/env-setup.sh"

    # 8. 기존 컨테이너 중지
    log_info "기존 컨테이너 중지 중..."
    CONTAINERS_REPLACED=true
    docker-compose down || log_warning "기존 컨테이너가 없습니다 (정상)"

    # 9. 이미지 정리
    log_info "사용하지 않는 이미지 정리 중..."
    docker image prune -f || log_warning "이미지 정리 실패 (계속 진행)"

    # 10. 컨테이너 시작
    log_info "Docker Compose 실행 중 (이미지 태그: $APP_IMAGE_TAG)..."
    docker-compose up -d || {
        log_error "Docker Compose 실행 실패!"
        fail_deploy 1
    }

    # 11. 컨테이너 상태 확인
    log_info "컨테이너 상태 확인 중..."
    sleep 5
    docker-compose ps

    if ! docker-compose ps | grep -q "Up"; then
        log_error "컨테이너 실행 실패!"
        log_info "컨테이너 로그:"
        docker-compose logs --tail=20 || true
        fail_deploy 1
    fi

    # 12. 헬스체크 실행
    log_info "헬스체크 실행 중..."
    if ! bash "$SCRIPT_DIR/health-check.sh"; then
        log_error "헬스체크 실패!"
        fail_deploy 1
    fi

    # 13. 성공 버전 기록 (다음 배포의 롤백 기준)
    mkdir -p "$DEPLOY_STATE_DIR" && echo "$APP_IMAGE_TAG" > "$LAST_SUCCESS_TAG_FILE" || {
        log_warning "성공 버전 기록 실패: $LAST_SUCCESS_TAG_FILE (다음 롤백은 이전 기록 기준)"
    }

    # 14. 배포 성공
    log_success "🎉 서버 배포 성공!"
    log_info "📝 브랜치: $DEPLOY_BRANCH"
    log_info "📝 커밋: $COMMIT_SHA"
    log_info "🐳 Docker 이미지: $APP_IMAGE"
    log_info "🕐 배포 시간: $(date)"
    log_info "🔄 롤백 기준 버전 갱신: $APP_IMAGE_TAG"

    # 임시 파일 정리
    rm -f "$SCRIPT_DIR"/*.tmp 2>/dev/null || true
}

# 메인 함수 실행
main "$@"
