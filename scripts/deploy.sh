#!/bin/bash

set -e  # 에러 발생 시 즉시 종료

# 스크립트 디렉토리
SCRIPT_DIR="/home/ubuntu/toktot-server/scripts"
PROJECT_DIR="/home/ubuntu/toktot-server"

# ✅ 배포 브랜치 설정 (환경변수로 받거나 기본값 main 사용)
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"

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

    # 롤백 실행
    if [ -f "$SCRIPT_DIR/rollback.sh" ]; then
        log_info "자동 롤백 시작..."
        bash "$SCRIPT_DIR/rollback.sh" || {
            log_error "롤백도 실패했습니다. 긴급 대응 필요!"
            send_emergency_alert "배포 실패 + 롤백 실패"
        }
    fi

    exit $exit_code
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
        exit 1
    fi

    # 1. 기본 검증
    if [ ! -d "$PROJECT_DIR" ]; then
        log_error "프로젝트 디렉토리를 찾을 수 없습니다: $PROJECT_DIR"
        exit 1
    fi

    cd "$PROJECT_DIR"

    # 2. Git 저장소 확인
    if [ ! -d ".git" ]; then
        log_error "Git 저장소를 찾을 수 없습니다!"
        exit 1
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
        exit 1
    }

    # ✅ 로컬 브랜치를 origin/main으로 강제 리셋
    git checkout $DEPLOY_BRANCH 2>/dev/null || git checkout -b $DEPLOY_BRANCH
    git reset --hard origin/$DEPLOY_BRANCH || {
        log_error "Git reset 실패!"
        exit 1
    }

    log_success "Git 저장소 업데이트 완료 (현재 브랜치: $(git rev-parse --abbrev-ref HEAD))"

    # 4. 필요한 디렉토리 생성
    log_info "필요한 디렉토리 생성 중..."
    mkdir -p nginx/conf.d certbot/www certbot/conf

    # 5. 현재 실행 중인 이미지 백업 (롤백용)
    log_info "현재 실행 중인 버전 백업 중..."
    CURRENT_IMAGE=""
    if docker-compose ps app 2>/dev/null | grep -q "Up"; then
        CURRENT_IMAGE=$(docker inspect $(docker-compose ps -q app) --format='{{index .Config.Image}}' 2>/dev/null || echo "")
        if [ ! -z "$CURRENT_IMAGE" ]; then
            log_info "백업용 이미지: $CURRENT_IMAGE"
            echo "$CURRENT_IMAGE" > /tmp/last_working_image.txt
        fi
    fi

    # 6. Docker 이미지 pull 및 검증 (✅ latest 태그 사용)
    log_info "최신 Docker 이미지 다운로드 중..."

    # ✅ main 브랜치는 latest 태그 사용
    DOCKER_IMAGE_TAG="latest"
    if [ "$DEPLOY_BRANCH" != "main" ]; then
        DOCKER_IMAGE_TAG="$DEPLOY_BRANCH"
    fi

    docker pull "$DOCKERHUB_USERNAME/toktot:$DOCKER_IMAGE_TAG" || {
        log_error "Docker 이미지 pull 실패!"
        exit 1
    }

    # Docker 이미지 무결성 검증
    log_info "Docker 이미지 무결성 검증 중..."
    if ! docker image inspect "$DOCKERHUB_USERNAME/toktot:$DOCKER_IMAGE_TAG" >/dev/null 2>&1; then
        log_error "Docker 이미지가 손상되었거나 존재하지 않습니다!"
        exit 1
    fi
    log_success "Docker 이미지 검증 완료"

    # 7. 기존 컨테이너 중지
    log_info "기존 컨테이너 중지 중..."
    docker-compose down || log_warning "기존 컨테이너가 없습니다 (정상)"

    # 8. 이미지 정리
    log_info "사용하지 않는 이미지 정리 중..."
    docker image prune -f || log_warning "이미지 정리 실패 (계속 진행)"

    # 9. 환경변수 설정 및 컨테이너 시작
    log_info "환경변수 설정 및 Docker Compose 실행 중..."
    source "$SCRIPT_DIR/env-setup.sh"
    docker-compose up -d || {
        log_error "Docker Compose 실행 실패!"
        exit 1
    }

    # 10. 컨테이너 상태 확인
    log_info "컨테이너 상태 확인 중..."
    sleep 5
    docker-compose ps

    if ! docker-compose ps | grep -q "Up"; then
        log_error "컨테이너 실행 실패!"
        log_info "컨테이너 로그:"
        docker-compose logs --tail=20
        exit 1
    fi

    # 11. 헬스체크 실행
    log_info "헬스체크 실행 중..."
    if ! bash "$SCRIPT_DIR/health-check.sh"; then
        log_error "헬스체크 실패!"
        exit 1
    fi

    # 12. 배포 성공
    log_success "🎉 서버 배포 성공!"
    log_info "📝 브랜치: $DEPLOY_BRANCH"
    log_info "📝 커밋: $COMMIT_SHA"
    log_info "🐳 Docker 이미지: $DOCKERHUB_USERNAME/toktot:$DOCKER_IMAGE_TAG"
    log_info "🕐 배포 시간: $(date)"
    log_info "🔄 롤백 시스템: 활성"

    # 임시 파일 정리
    rm -f "$SCRIPT_DIR"/*.tmp 2>/dev/null || true
}

# 메인 함수 실행
main "$@"