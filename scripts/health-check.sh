#!/bin/bash

set -e

PROJECT_DIR="/home/ubuntu/toktot-server"
MAX_WAIT_TIME=${1:-120}

# 로그 함수
log_info() {
    echo "🔍 [$(date '+%Y-%m-%d %H:%M:%S')] $1"
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

# 메인 헬스체크 함수
main() {
    log_info "🔍 TokTot Dev 서버 헬스체크 시작..."
    log_info "최대 대기 시간: ${MAX_WAIT_TIME}초"

    cd "$PROJECT_DIR"

    # 1. 컨테이너 상태 확인
    log_info "컨테이너 상태 확인 중..."
    if ! docker-compose ps | grep -q "Up"; then
        log_error "컨테이너가 실행되지 않음"
        return 1
    fi
    log_success "모든 컨테이너 정상 실행 중"

    # 2. HTTP 헬스체크 (가장 중요 - Spring Actuator가 DB/Redis도 체크함)
    log_info "HTTP 헬스체크 실행 중..."
    local wait_time=0
    local check_interval=5

    while [ $wait_time -lt $MAX_WAIT_TIME ]; do
        if timeout 15 curl -f -s http://localhost/actuator/health > /dev/null 2>&1; then
            log_success "Spring Boot 헬스체크 성공! (${wait_time}초 소요)"
            break
        fi

        if [ $((wait_time % 30)) -eq 0 ] && [ $wait_time -gt 0 ]; then
            log_info "서버 시작 중... (${wait_time}초 경과)"
        fi

        sleep $check_interval
        wait_time=$((wait_time + check_interval))
    done

    if [ $wait_time -ge $MAX_WAIT_TIME ]; then
        log_error "HTTP 헬스체크 타임아웃 (${MAX_WAIT_TIME}초)"
        # 실패 시 로그 출력
        log_error "애플리케이션 로그:"
        docker-compose logs --tail=30 app || log_error "앱 로그 확인 불가"
        return 1
    fi

    # 3. 추가 엔드포인트 확인 (선택사항)
    if timeout 10 curl -f -s http://localhost/actuator/info > /dev/null 2>&1; then
        log_success "애플리케이션 정보 엔드포인트 정상"
    else
        log_warning "정보 엔드포인트 접근 실패 (중요하지 않음)"
    fi

    # 4. 종합 리포트
    log_success "================================"
    log_success "🎉 모든 헬스체크 통과!"
    log_success "================================"
    docker-compose ps
    return 0
}

# 메인 함수 실행
main "$@"
