#!/bin/bash

# 스크립트 디렉토리와 프로젝트 루트 경로 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Docker 컨테이너 설정
CONTAINER_NAME="toktot-dev-app"
COMPOSE_SERVICE="app"
LOG_DIR="/app/logs"

# docker-compose 명령어 래퍼 (항상 프로젝트 루트에서 실행)
function docker_compose() {
    (cd "$PROJECT_ROOT" && docker-compose "$@")
}

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# 헤더 출력
function print_header() {
    echo -e "${CYAN}================================${NC}"
    echo -e "${WHITE}🚀 TokTot App 로그 분석 도구${NC}"
    echo -e "${CYAN}================================${NC}"
    echo ""
}

# 실시간 app 로그 확인
function tail_app_logs() {
    echo -e "${CYAN}📋 실시간 App 로그 확인 중...${NC}"
    echo -e "${YELLOW}종료하려면 Ctrl+C를 누르세요.${NC}"
    echo ""

    docker_compose logs -f $COMPOSE_SERVICE
}

# 최근 app 로그 확인
function show_recent_logs() {
    echo -e "${GREEN}📜 최근 App 로그 (100줄):${NC}"
    echo ""

    docker_compose logs --tail=100 $COMPOSE_SERVICE
}

# 에러 로그 확인
function show_errors() {
    echo -e "${RED}🚨 에러 로그 확인${NC}"
    echo ""

    while true; do
        echo -e "${YELLOW}옵션을 선택하세요:${NC}"
        echo "1. 최근 20개 에러"
        echo "2. 최근 50개 에러"
        echo "3. 최근 100개 에러"
        echo "4. 특정 개수 입력"
        echo "5. 시간 범위로 필터링"
        echo "6. 키워드로 에러 검색"
        echo "0. 이전 메뉴로"
        echo ""
        echo -n "선택: "
        read error_choice

        case $error_choice in
            1|2|3)
                local count=""
                case $error_choice in
                    1) count=20 ;;
                    2) count=50 ;;
                    3) count=100 ;;
                esac
                echo -e "${RED}📋 최근 ${count}개 에러:${NC}"
                docker_compose logs $COMPOSE_SERVICE | grep -i "error\|exception\|warn" | tail -$count
                ;;
            4)
                echo -n "표시할 에러 개수를 입력하세요: "
                read error_count
                if [[ "$error_count" =~ ^[0-9]+$ ]]; then
                    echo -e "${RED}📋 최근 ${error_count}개 에러:${NC}"
                    docker_compose logs $COMPOSE_SERVICE | grep -i "error\|exception\|warn" | tail -$error_count
                else
                    echo -e "${RED}❌ 숫자만 입력해주세요.${NC}"
                fi
                ;;
            5)
                echo -n "시간 범위를 입력하세요 (예: 30m, 1h): "
                read time_range
                if [ ! -z "$time_range" ]; then
                    echo -e "${RED}📋 최근 ${time_range} 에러:${NC}"
                    docker_compose logs --since="$time_range" $COMPOSE_SERVICE | grep -i "error\|exception\|warn"
                else
                    echo -e "${RED}❌ 시간 범위를 입력해주세요.${NC}"
                fi
                ;;
            6)
                echo -n "검색할 키워드를 입력하세요: "
                read keyword
                if [ ! -z "$keyword" ]; then
                    echo -e "${RED}📋 '$keyword' 관련 에러:${NC}"
                    docker_compose logs $COMPOSE_SERVICE | grep -i "error\|exception\|warn" | grep -i "$keyword" | tail -30
                else
                    echo -e "${RED}❌ 키워드를 입력해주세요.${NC}"
                fi
                ;;
            0) break ;;
            *) echo -e "${RED}❌ 잘못된 선택입니다.${NC}" ;;
        esac

        [ "$error_choice" != "0" ] && { echo ""; echo -e "${YELLOW}계속하려면 Enter를 누르세요...${NC}"; read; }
    done
}

# JSON 관련 에러 확인
function show_json_errors() {
    echo -e "${RED}🔍 JSON 관련 에러 확인:${NC}"
    echo ""

    local json_errors=$(docker_compose logs $COMPOSE_SERVICE | \
    grep -i "json.*parsing\|parsing.*error\|HttpMessageNotReadableException\|content-type")

    if [ -z "$json_errors" ]; then
        echo -e "${GREEN}✅ JSON 관련 에러가 없습니다.${NC}"
    else
        echo -e "${YELLOW}JSON parsing 관련 에러:${NC}"
        echo "$json_errors" | tail -20
    fi
}

# 로그 검색 기능
function search_logs() {
    echo -e "${BLUE}🔍 로그 검색${NC}"
    echo ""

    echo -n "검색할 키워드를 입력하세요: "
    read keyword

    if [ -z "$keyword" ]; then
        echo -e "${RED}❌ 키워드가 입력되지 않았습니다.${NC}"
        return 1
    fi

    echo -e "${BLUE}📋 '$keyword' 검색 결과 (최근 20개):${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep -i "$keyword" | tail -20
}

# 특정 API 엔드포인트 분석
function analyze_specific_endpoint() {
    echo -n "분석할 API 엔드포인트를 입력하세요 (예: /v1/reviews/search): "
    read endpoint

    if [ -z "$endpoint" ]; then
        echo -e "${RED}❌ 엔드포인트가 입력되지 않았습니다.${NC}"
        return 1
    fi

    echo -e "${BLUE}🔍 $endpoint 엔드포인트 분석:${NC}"
    echo ""

    echo -e "${GREEN}📋 $endpoint 관련 로그 (최근 20개):${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep "$endpoint" | tail -20

    echo ""
    echo -e "${RED}📋 $endpoint 에러 로그:${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep "$endpoint" | grep -i "error\|exception\|warn" | tail -10
}

# 시간대별 로그 검색
function search_logs_by_time() {
    echo -e "${BLUE}🕐 시간대별 로그 검색${NC}"
    echo ""

    echo -n "시간 범위를 입력하세요 (예: 30m, 1h, 2h): "
    read time_range

    if [ -z "$time_range" ]; then
        echo -e "${RED}❌ 시간 범위가 입력되지 않았습니다.${NC}"
        return 1
    fi

    echo -e "${BLUE}📋 최근 $time_range 로그:${NC}"
    docker_compose logs --since="$time_range" $COMPOSE_SERVICE | tail -50
}

# 특정 사용자 로그 검색
function search_user_logs() {
    echo -n "사용자 ID 입력: "
    read user_id

    if [ -z "$user_id" ]; then
        echo -e "${RED}❌ 사용자 ID가 입력되지 않았습니다.${NC}"
        return 1
    fi

    echo -e "${BLUE}👤 사용자 $user_id 관련 로그:${NC}"
    echo ""

    docker_compose logs $COMPOSE_SERVICE | grep "userId.*$user_id\|user.*$user_id" | tail -20
}

# HTTP 요청/응답 분석
function analyze_http_requests() {
    echo -e "${PURPLE}🌐 HTTP 요청/응답 분석:${NC}"
    echo ""

    echo -e "${YELLOW}HTTP 요청 (최근 15개):${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep -E "POST|GET|PUT|DELETE" | tail -15

    echo ""
    echo -e "${YELLOW}HTTP 에러 응답 (최근 10개):${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep -E "HTTP.*[4-5][0-9][0-9]|status.*[4-5][0-9][0-9]" | tail -10
}

# 실시간 에러 모니터링
function monitor_errors_realtime() {
    echo -e "${RED}🚨 실시간 에러 모니터링${NC}"
    echo -e "${YELLOW}종료하려면 Ctrl+C를 누르세요.${NC}"
    echo ""

    docker_compose logs -f $COMPOSE_SERVICE | grep --line-buffered -i "error\|exception\|warn" | while read line; do
        echo -e "${RED}[ERROR]${NC} $line"
    done
}

# 성능 분석
function analyze_performance() {
    echo -e "${YELLOW}⚡ 성능 분석:${NC}"
    echo ""

    echo -e "${BLUE}느린 요청 (duration 관련):${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep -i "duration\|took\|ms\|seconds" | tail -10

    echo ""
    echo -e "${BLUE}데이터베이스 관련:${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep -i "sql\|hibernate\|database" | tail -5

    echo ""
    echo -e "${BLUE}메모리/성능 경고:${NC}"
    docker_compose logs $COMPOSE_SERVICE | grep -i "memory\|heap\|gc\|performance" | tail -5
}

# 컨테이너 상태 및 헬스체크
function check_container_health() {
    echo -e "${GREEN}🏥 컨테이너 상태 및 헬스체크:${NC}"
    echo ""

    echo -e "${BLUE}컨테이너 상태:${NC}"
    docker_compose ps

    echo ""
    echo -e "${BLUE}헬스체크:${NC}"
    if curl -f -s http://localhost/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 헬스체크 성공${NC}"
        curl -s http://localhost/actuator/health | jq '.' 2>/dev/null || echo "응답 있음"
    else
        echo -e "${RED}❌ 헬스체크 실패${NC}"
    fi

    echo ""
    echo -e "${BLUE}시스템 리소스:${NC}"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep toktot || echo "리소스 정보 확인 불가"
}

# 컨테이너 내부 로그 파일 확인
function check_internal_logs() {
    echo -e "${CYAN}📁 컨테이너 내부 로그 파일 확인:${NC}"
    echo ""

    echo -e "${BLUE}로그 디렉토리 구조:${NC}"
    docker exec $CONTAINER_NAME ls -la /app/logs/ 2>/dev/null || echo "내부 로그 디렉토리 없음"

    echo ""
    echo -e "${BLUE}Spring Boot 로그 설정:${NC}"
    docker exec $CONTAINER_NAME cat /app/logs/application.json 2>/dev/null | tail -5 || echo "application.json 없음"
}

# 메뉴 출력
function show_menu() {
    print_header
    echo -e "${WHITE}🔥 실시간 모니터링${NC}"
    echo -e "${GREEN}1.${NC} 📺 실시간 App 로그    ${RED}2.${NC} 🚨 실시간 에러 모니터링"
    echo ""
    echo -e "${WHITE}📊 로그 분석${NC}"
    echo -e "${YELLOW}3.${NC} 📜 최근 App 로그      ${RED}4.${NC} 🚨 에러 로그 확인"
    echo -e "${PURPLE}5.${NC} 🔍 JSON 에러 확인"
    echo ""
    echo -e "${WHITE}🔍 검색 & 분석${NC}"
    echo -e "${BLUE}6.${NC} 🔍 로그 검색         ${CYAN}7.${NC} 🎯 API 엔드포인트 분석"
    echo -e "${BLUE}8.${NC} 🕐 시간대별 로그      ${BLUE}9.${NC} 👤 사용자 로그"
    echo ""
    echo -e "${WHITE}📈 성능 & 시스템${NC}"
    echo -e "${YELLOW}10.${NC} ⚡ 성능 분석        ${PURPLE}11.${NC} 🌐 HTTP 요청/응답"
    echo -e "${GREEN}12.${NC} 🏥 헬스체크         ${CYAN}13.${NC} 📁 내부 로그 확인"
    echo ""
    echo -e "${RED}0.${NC} 종료"
    echo -e "${CYAN}================================${NC}"
    echo ""
}

# 컨테이너 상태 확인
function check_container() {
    if ! docker ps | grep -q $CONTAINER_NAME; then
        echo -e "${RED}❌ 컨테이너 '$CONTAINER_NAME'가 실행 중이지 않습니다.${NC}"
        echo "다음 명령어로 컨테이너를 시작하세요:"
        echo "  docker-compose up -d"
        exit 1
    fi
}

# 의존성 확인
function check_dependencies() {
    # Docker Compose 확인
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}❌ docker-compose가 설치되지 않았습니다.${NC}"
        exit 1
    fi

    # 프로젝트 루트에 docker-compose.yml 파일 확인
    if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ]; then
        echo -e "${RED}❌ docker-compose.yml 파일을 찾을 수 없습니다.${NC}"
        echo -e "${YELLOW}예상 위치: $PROJECT_ROOT/docker-compose.yml${NC}"
        exit 1
    fi
}

# 빠른 실행 명령어들
function show_quick_commands() {
    echo -e "${CYAN}🚀 빠른 명령어 가이드:${NC}"
    echo ""
    echo -e "${BLUE}현재 경로 정보:${NC}"
    echo -e "${YELLOW}프로젝트 루트:${NC} $PROJECT_ROOT"
    echo ""
    echo -e "${GREEN}빠른 명령어:${NC}"
    echo -e "${GREEN}실시간 로그:${NC} ./scripts/log-management.sh tail"
    echo -e "${RED}에러 확인:${NC} ./scripts/log-management.sh errors"
    echo -e "${BLUE}JSON 에러:${NC} ./scripts/log-management.sh json"
    echo -e "${CYAN}검색:${NC} ./scripts/log-management.sh search"
    echo ""
}

# 메인 실행 함수
function main() {
    # 초기 검사
    check_dependencies
    check_container

    # 명령행 인수가 있는 경우 직접 실행
    if [ "$1" ]; then
        case "$1" in
            "tail"|"live") tail_app_logs ;;
            "recent") show_recent_logs ;;
            "errors") show_errors ;;
            "json") show_json_errors ;;
            "search") search_logs ;;
            "endpoint") analyze_specific_endpoint ;;
            "user") search_user_logs ;;
            "http") analyze_http_requests ;;
            "performance") analyze_performance ;;
            "health") check_container_health ;;
            "internal") check_internal_logs ;;
            "time") search_logs_by_time ;;
            "monitor") monitor_errors_realtime ;;
            "help") show_quick_commands ;;
            *)
                echo -e "${RED}알 수 없는 명령어: $1${NC}"
                show_quick_commands
                ;;
        esac
        return
    fi

    # 대화형 메뉴
    while true; do
        show_menu
        echo -n "선택하세요 (0-13): "
        read choice

        case $choice in
            1) tail_app_logs ;;
            2) monitor_errors_realtime ;;
            3) show_recent_logs ;;
            4) show_errors ;;
            5) show_json_errors ;;
            6) search_logs ;;
            7) analyze_specific_endpoint ;;
            8) search_logs_by_time ;;
            9) search_user_logs ;;
            10) analyze_performance ;;
            11) analyze_http_requests ;;
            12) check_container_health ;;
            13) check_internal_logs ;;
            0)
                echo -e "${GREEN}👋 로그 분석을 종료합니다.${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ 잘못된 선택입니다. 0-13 사이의 숫자를 입력하세요.${NC}"
                ;;
        esac

        echo ""
        echo -e "${YELLOW}계속하려면 Enter를 누르세요...${NC}"
        read
    done
}

# 스크립트 실행
main "$@"
