#!/bin/bash

set -e

# 로그 함수
log_info() {
    echo "🔧 [$(date '+%Y-%m-%d %H:%M:%S')] $1"
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

# 환경변수 파일 경로 (수정됨)
ENV_FILE="/home/ubuntu/.toktot-env"
PROJECT_ENV_FILE="/home/ubuntu/toktot-server/.env"

main() {
    log_info "환경변수 설정 중..."

    # GitHub Actions에서 전달된 환경변수들 백업 (최고 우선순위)
    local github_vars=()
    if [ -n "$DOCKERHUB_USERNAME" ]; then
        github_vars+=("DOCKERHUB_USERNAME=$DOCKERHUB_USERNAME")
    fi
    if [ -n "$JWT_SECRET" ]; then
        github_vars+=("JWT_SECRET=$JWT_SECRET")
    fi
    if [ -n "$POSTGRES_PASSWORD" ]; then
        github_vars+=("POSTGRES_PASSWORD=$POSTGRES_PASSWORD")
    fi
    if [ -n "$REDIS_PASSWORD" ]; then
        github_vars+=("REDIS_PASSWORD=$REDIS_PASSWORD")
    fi
    if [ -n "$AWS_ACCESS_KEY_ID" ]; then
        github_vars+=("AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID")
    fi
    if [ -n "$AWS_SECRET_ACCESS_KEY" ]; then
        github_vars+=("AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY")
    fi

    # 1. 서버 환경변수 파일 로드 (낮은 우선순위)
    if [ -f "$ENV_FILE" ]; then
        log_info "서버 환경변수 파일 로드: $ENV_FILE"
        source "$ENV_FILE"
        log_success "서버 환경변수 로드 완료"
    else
        log_warning "서버 환경변수 파일을 찾을 수 없음: $ENV_FILE"
    fi

    # 2. 프로젝트 .env 파일 로드 (중간 우선순위)
    if [ -f "$PROJECT_ENV_FILE" ]; then
        log_info "프로젝트 환경변수 파일 로드: $PROJECT_ENV_FILE"
        source "$PROJECT_ENV_FILE"
        log_success "프로젝트 환경변수 로드 완료"
    else
        log_warning "프로젝트 환경변수 파일을 찾을 수 없음: $PROJECT_ENV_FILE"
    fi

    # 3. GitHub Actions 환경변수 재적용 (최고 우선순위 - 덮어쓰기)
    log_info "GitHub Actions 환경변수 우선 적용 중..."
    for var in "${github_vars[@]}"; do
        export "$var"
        local var_name="${var%%=*}"
        log_success "$var_name 우선 적용됨"
    done

    # 4. 필수 환경변수 확인 및 기본값 설정
    log_info "필수 환경변수 확인 중..."

    # Docker 관련 (필수)
    if [ -z "$DOCKERHUB_USERNAME" ]; then
        log_error "DOCKERHUB_USERNAME이 설정되지 않았습니다!"
        return 1
    fi

    # Spring 프로파일
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

    # 데이터베이스 (필수)
    if [ -z "$POSTGRES_PASSWORD" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 POSTGRES_PASSWORD가 반드시 설정되어야 합니다!"
            return 1
        else
            log_warning "POSTGRES_PASSWORD가 설정되지 않음. 개발용 기본값 사용"
            export POSTGRES_PASSWORD="devpassword123"
        fi
    fi
    export DB_PASSWORD="${DB_PASSWORD:-$POSTGRES_PASSWORD}"

    # Redis
    if [ -z "$REDIS_PASSWORD" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 REDIS_PASSWORD가 반드시 설정되어야 합니다!"
            return 1
        else
            export REDIS_PASSWORD="devredis123"
        fi
    fi

    # JWT (필수)
    if [ -z "$JWT_SECRET" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 JWT_SECRET이 반드시 설정되어야 합니다!"
            return 1
        else
            log_warning "JWT_SECRET이 설정되지 않음. 개발용 기본값 사용"
            export JWT_SECRET="dev-jwt-secret-key-change-in-production-please-make-it-longer-than-256-bits"
        fi
    fi

    # 카카오 API (선택사항)
    export KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID:-}"
    export KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET:-}"
    export KAKAO_REST_API_KEY="${KAKAO_REST_API_KEY:-}"

    # 메일 설정 (선택사항)
    export MAIL_USERNAME="${MAIL_USERNAME:-}"
    export MAIL_PASSWORD="${MAIL_PASSWORD:-}"

    # AWS 설정 (필수)
    if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 AWS 설정이 반드시 필요합니다!"
            return 1
        else
            log_warning "AWS 설정이 없습니다. 일부 기능이 제한될 수 있습니다."
        fi
    fi
    export S3_BUCKET_NAME="${S3_BUCKET_NAME:-}"

    # Tour API (선택사항)
    export TOUR_API_SERVICE_KEY="${TOUR_API_SERVICE_KEY:-}"

    # 5. 환경변수 검증
    local required_vars=("DOCKERHUB_USERNAME")
    local missing_vars=()

    # 운영환경에서는 더 엄격한 검증
    if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
        required_vars+=("POSTGRES_PASSWORD" "JWT_SECRET" "AWS_ACCESS_KEY_ID" "AWS_SECRET_ACCESS_KEY")
    fi

    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            missing_vars+=("$var")
        fi
    done

    if [ ${#missing_vars[@]} -gt 0 ]; then
        log_error "필수 환경변수가 설정되지 않음: ${missing_vars[*]}"
        return 1
    fi

    # 6. 환경변수 설정 완료 로그
    log_success "환경변수 설정 완료"
    log_info "Spring Profile: $SPRING_PROFILES_ACTIVE"
    log_info "Docker Username: $DOCKERHUB_USERNAME"

    # 보안을 위해 민감한 정보는 마스킹
    if [ -n "$AWS_ACCESS_KEY_ID" ]; then
        log_info "AWS Access Key: ${AWS_ACCESS_KEY_ID:0:8}***"
    fi

    if [ -n "$KAKAO_CLIENT_ID" ]; then
        log_info "Kakao Client ID: ${KAKAO_CLIENT_ID:0:8}***"
    fi

    # JWT Secret 길이만 표시 (값은 노출하지 않음)
    if [ -n "$JWT_SECRET" ]; then
        local jwt_length=${#JWT_SECRET}
        log_info "JWT Secret 길이: ${jwt_length} characters"
        if [ $jwt_length -lt 32 ]; then
            log_warning "JWT Secret이 너무 짧습니다. 최소 32자 이상 권장"
        fi
    fi

    return 0
}

# 메인 함수 실행
main "$@"
