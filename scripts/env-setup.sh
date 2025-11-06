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

# 환경변수 파일 경로
ENV_FILE="/home/ubuntu/.toktot-env"
PROJECT_ENV_FILE="/home/ubuntu/toktot-server/.env"

main() {
    log_info "환경변수 설정 중..."

    local github_vars=()
    if [ -n "$DOCKERHUB_USERNAME" ]; then
        github_vars+=("DOCKERHUB_USERNAME=$DOCKERHUB_USERNAME")
    fi
    if [ -n "$FIREBASE_SERVICE_ACCOUNT_KEY_BASE64" ]; then
        github_vars+=("FIREBASE_SERVICE_ACCOUNT_KEY_BASE64=$FIREBASE_SERVICE_ACCOUNT_KEY_BASE64")
    fi
    if [ -n "$FIREBASE_PROJECT_ID" ]; then
        github_vars+=("FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID")
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

    if [ -f "$ENV_FILE" ]; then
        log_info "서버 환경변수 파일 로드: $ENV_FILE"
        source "$ENV_FILE"
        log_success "서버 환경변수 로드 완료"
    else
        log_warning "서버 환경변수 파일을 찾을 수 없음: $ENV_FILE"
    fi

    if [ -f "$PROJECT_ENV_FILE" ]; then
        log_info "프로젝트 환경변수 파일 로드: $PROJECT_ENV_FILE"
        source "$PROJECT_ENV_FILE"
        log_success "프로젝트 환경변수 로드 완료"
    else
        log_warning "프로젝트 환경변수 파일을 찾을 수 없음: $PROJECT_ENV_FILE"
    fi

    log_info "GitHub Actions 환경변수 우선 적용 중..."
    for var in "${github_vars[@]}"; do
        export "$var"
        local var_name="${var%%=*}"
        log_success "$var_name 우선 적용됨"
    done

    log_info "필수 환경변수 확인 중..."

    if [ -z "$DOCKERHUB_USERNAME" ]; then
        log_error "DOCKERHUB_USERNAME이 설정되지 않았습니다!"
        return 1
    fi

    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

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

    if [ -z "$REDIS_PASSWORD" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 REDIS_PASSWORD가 반드시 설정되어야 합니다!"
            return 1
        else
            export REDIS_PASSWORD="devredis123"
        fi
    fi

    if [ -z "$JWT_SECRET" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 JWT_SECRET이 반드시 설정되어야 합니다!"
            return 1
        else
            log_warning "JWT_SECRET이 설정되지 않음. 개발용 기본값 사용"
            export JWT_SECRET="dev-jwt-secret-key-change-in-production-please-make-it-longer-than-256-bits"
        fi
    fi

    export KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID:-}"
    export KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET:-}"
    export KAKAO_REST_API_KEY="${KAKAO_REST_API_KEY:-}"

    export MAIL_USERNAME="${MAIL_USERNAME:-}"
    export MAIL_PASSWORD="${MAIL_PASSWORD:-}"

    export FIREBASE_SERVICE_ACCOUNT_KEY_BASE64="${FIREBASE_SERVICE_ACCOUNT_KEY_BASE64:-}"
    export FIREBASE_PROJECT_ID="${FIREBASE_PROJECT_ID:-}"


    if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
        if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
            log_error "운영환경에서는 AWS 설정이 반드시 필요합니다!"
            return 1
        else
            log_warning "AWS 설정이 없습니다. 일부 기능이 제한될 수 있습니다."
        fi
    fi
    export S3_BUCKET_NAME="${S3_BUCKET_NAME:-}"

    export TOUR_API_SERVICE_KEY="${TOUR_API_SERVICE_KEY:-}"

    local required_vars=("DOCKERHUB_USERNAME")
    local missing_vars=()

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

    log_success "환경변수 설정 완료"
    log_info "Spring Profile: $SPRING_PROFILES_ACTIVE"
    log_info "Docker Username: $DOCKERHUB_USERNAME"

    if [ -n "$AWS_ACCESS_KEY_ID" ]; then
        log_info "AWS Access Key: ${AWS_ACCESS_KEY_ID:0:8}***"
    fi

    if [ -n "$KAKAO_CLIENT_ID" ]; then
        log_info "Kakao Client ID: ${KAKAO_CLIENT_ID:0:8}***"
    fi

    if [ -n "$JWT_SECRET" ]; then
        local jwt_length=${#JWT_SECRET}
        log_info "JWT Secret 길이: ${jwt_length} characters"
        if [ $jwt_length -lt 32 ]; then
            log_warning "JWT Secret이 너무 짧습니다. 최소 32자 이상 권장"
        fi
    fi

    return 0
}

main "$@"
