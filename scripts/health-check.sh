#!/bin/bash

set -e

PROJECT_DIR="/home/ubuntu/toktot-server"
MAX_WAIT_TIME=${1:-120}  # 기본 120초, 매개변수로 변경 가능

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

# 컨테이너 상태 확인
check_containers() {
    log_info "컨테이너 상태 확인 중..."

    cd "$PROJECT_DIR"

    # 필수 컨테이너들이 실행 중인지 확인
    local required_services=("app" "postgres" "redis")
    local all_running=true

    for service in "${required_services[@]}"; do
        if docker-compose ps "$service" | grep -q "Up"; then
            log_success "$service 컨테이너 정상 실행 중"
        else
            log_error "$service 컨테이너가 실행되지 않음"
            all_running=false
        fi
    done

    # nginx는 선택적 (로컬 환경에서는 없을 수 있음)
    if docker-compose ps nginx 2>/dev/null | grep -q "Up"; then
        log_success "nginx 컨테이너 정상 실행 중"
    else
        log_warning "nginx 컨테이너 없음 (로컬 환경일 수 있음)"
    fi

    return $($all_running && echo 0 || echo 1)
}

# 네트워크 연결 확인 (재시도 로직 포함)
check_network_connectivity() {
    log_info "네트워크 연결 확인 중..."

    cd "$PROJECT_DIR"

    # 컨테이너 준비 상태 먼저 확인
    if ! docker-compose ps app | grep -q "Up"; then
        log_error "앱 컨테이너가 실행되지 않음"
        return 1
    fi

    # PostgreSQL 연결 확인 (재시도 로직)
    log_info "PostgreSQL 연결 확인 중..."
    local pg_connected=false
    for i in {1..3}; do
        if timeout 10 docker-compose exec -T app sh -c "echo > /dev/tcp/postgres/5432" 2>/dev/null; then
            log_success "PostgreSQL 연결 정상 (${i}번째 시도)"
            pg_connected=true
            break
        fi
        if [ $i -lt 3 ]; then
            log_warning "PostgreSQL 연결 실패, 재시도 중... (${i}/3)"
            sleep 3
        fi
    done

    if [ "$pg_connected" = false ]; then
        log_error "PostgreSQL 연결 실패 (3회 시도 후 포기)"
        return 1
    fi

    # Redis 연결 확인 (재시도 로직)
    log_info "Redis 연결 확인 중..."
    local redis_connected=false
    for i in {1..3}; do
        if timeout 10 docker-compose exec -T app sh -c "echo > /dev/tcp/redis/6379" 2>/dev/null; then
            log_success "Redis 연결 정상 (${i}번째 시도)"
            redis_connected=true
            break
        fi
        if [ $i -lt 3 ]; then
            log_warning "Redis 연결 실패, 재시도 중... (${i}/3)"
            sleep 3
        fi
    done

    if [ "$redis_connected" = false ]; then
        log_error "Redis 연결 실패 (3회 시도 후 포기)"
        return 1
    fi

    return 0
}

# 환경변수 확인
check_environment_variables() {
    log_info "필수 환경변수 확인 중..."

    cd "$PROJECT_DIR"

    local env_check_passed=true
    local required_envs=("AWS_ACCESS_KEY_ID" "JWT_SECRET" "SPRING_PROFILES_ACTIVE")

    for env_var in "${required_envs[@]}"; do
        if timeout 10 docker-compose exec -T app env 2>/dev/null | grep -q "$env_var"; then
            log_success "$env_var 환경변수 확인됨"
        else
            log_error "$env_var 환경변수 누락!"
            env_check_passed=false
        fi
    done

    return $($env_check_passed && echo 0 || echo 1)
}

# HTTP 헬스체크 (타임아웃 개선)
check_http_health() {
    log_info "HTTP 헬스체크 실행 중..."

    local wait_time=0
    local check_interval=5

    while [ $wait_time -lt $MAX_WAIT_TIME ]; do
        # Spring Boot Actuator 헬스체크 (타임아웃 설정)
        if timeout 15 curl -f -s http://localhost/actuator/health > /dev/null 2>&1; then
            log_success "Spring Boot 헬스체크 성공! (${wait_time}초 소요)"
            break
        fi

        # 중간 진행 상황 알림
        if [ $((wait_time % 30)) -eq 0 ] && [ $wait_time -gt 0 ]; then
            log_info "서버 시작 중... (${wait_time}초 경과)"

            # 컨테이너 상태 재확인
            cd "$PROJECT_DIR"
            if ! docker-compose ps | grep -E "(app|nginx)" | grep -q "Up"; then
                log_error "컨테이너가 중단됨"
                return 1
            fi
        fi

        sleep $check_interval
        wait_time=$((wait_time + check_interval))
    done

    if [ $wait_time -ge $MAX_WAIT_TIME ]; then
        log_error "HTTP 헬스체크 타임아웃 (${MAX_WAIT_TIME}초)"

        # 실패 시 로그 출력
        cd "$PROJECT_DIR"
        log_error "애플리케이션 로그:"
        docker-compose logs --tail=30 app || log_error "앱 로그 확인 불가"

        return 1
    fi

    return 0
}

# 추가 엔드포인트 확인 (타임아웃 추가)
check_additional_endpoints() {
    log_info "추가 엔드포인트 확인 중..."

    # Spring Boot Info 엔드포인트
    if timeout 10 curl -f -s http://localhost/actuator/info > /dev/null 2>&1; then
        log_success "애플리케이션 정보 엔드포인트 정상"
    else
        log_warning "정보 엔드포인트 접근 실패 (중요하지 않음)"
    fi

    # API 기본 엔드포인트 (있다면)
    if timeout 10 curl -f -s http://localhost/api/v1/health > /dev/null 2>&1; then
        log_success "API 헬스체크 엔드포인트 정상"
    else
        log_warning "API 헬스체크 엔드포인트 없음 (선택사항)"
    fi

    return 0
}

# 종합 리포트 생성
generate_health_report() {
    log_info "================================"
    log_info "헬스체크 종합 리포트"
    log_info "================================"

    cd "$PROJECT_DIR"

    # 컨테이너 상태
    echo "📊 컨테이너 상태:"
    docker-compose ps

    # 시스템 리소스
    echo ""
    log_info "💻 시스템 리소스 사용량:"
    timeout 10 docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null || echo "리소스 정보 확인 불가"

    # 디스크 사용량
    echo ""
    log_info "💾 디스크 사용량:"
    df -h / 2>/dev/null || echo "디스크 정보 확인 불가"

    # 네트워크 포트 상태
    echo ""
    log_info "🌐 네트워크 포트 상태:"
    ss -tuln | grep -E ":(80|443|8080|5432|6379)" 2>/dev/null || echo "포트 정보 확인 불가"

    log_info "================================"
}

# 메인 헬스체크 함수
main() {
    local overall_status=0

    log_info "🔍 TokTot Dev 서버 헬스체크 시작..."
    log_info "최대 대기 시간: ${MAX_WAIT_TIME}초"

    # 1. 컨테이너 상태 확인
    if ! check_containers; then
        log_error "컨테이너 상태 확인 실패"
        overall_status=1
    fi

    # 2. 네트워크 연결 확인
    if ! check_network_connectivity; then
        log_error "네트워크 연결 확인 실패"
        overall_status=1
    fi

    # 3. 환경변수 확인
    if ! check_environment_variables; then
        log_error "환경변수 확인 실패"
        overall_status=1
    fi

    # 4. HTTP 헬스체크 (가장 중요)
    if ! check_http_health; then
        log_error "HTTP 헬스체크 실패"
        overall_status=1
    fi

    # 5. 추가 엔드포인트 확인 (선택사항)
    check_additional_endpoints || true  # 실패해도 전체 결과에 영향 안 줌

    # 6. 종합 리포트
    generate_health_report

    # 결과 반환
    if [ $overall_status -eq 0 ]; then
        log_success "🎉 모든 헬스체크 통과!"
        return 0
    else
        log_error "❌ 헬스체크 실패!"
        return 1
    fi
}

# 메인 함수 실행
main "$@"
