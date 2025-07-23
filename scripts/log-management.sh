LOG_DIR="/app/logs"
CONTAINER_NAME="toktot-dev-app"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 실시간 로그 확인
function tail_logs() {
    echo -e "${CYAN}📋 실시간 애플리케이션 로그 확인 중...${NC}"
    echo "종료하려면 Ctrl+C를 누르세요."
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/application.json; then
        docker exec $CONTAINER_NAME tail -f $LOG_DIR/application.json | while read line; do
            echo "$line" | jq -C '.' 2>/dev/null || echo "$line"
        done
    else
        echo -e "${RED}❌ 로그 파일이 존재하지 않습니다: $LOG_DIR/application.json${NC}"
    fi
}

# 에러 로그만 확인
function show_errors() {
    echo -e "${RED}🚨 최근 에러 로그 (10개):${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/error.json; then
        docker exec $CONTAINER_NAME tail -50 $LOG_DIR/error.json | \
        jq -r 'select(.["log.level"] == "ERROR") |
               "\(.["@timestamp"]) [\(.["log.level"])] \(.message) (userId: \(.userId // "N/A"), requestId: \(.requestId // "N/A"))"' | \
        tail -10
    else
        echo -e "${YELLOW}⚠️  에러 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 특정 사용자 로그 검색
function search_user_logs() {
    local user_id=$1
    if [ -z "$user_id" ]; then
        echo -e "${RED}사용법: search_user_logs <user_id>${NC}"
        return 1
    fi

    echo -e "${BLUE}👤 사용자 $user_id 관련 로그:${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/application.json; then
        docker exec $CONTAINER_NAME grep "\"userId\":\"$user_id\"" $LOG_DIR/application.json | \
        jq -r '"\(.["@timestamp"]) [\(.["log.level"])] \(.message)"' | \
        tail -20
    else
        echo -e "${YELLOW}⚠️  애플리케이션 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 성능 분석 (응답시간 5초 이상)
function analyze_slow_requests() {
    echo -e "${YELLOW}🐌 느린 요청 분석 (5초 이상):${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/application.json; then
        docker exec $CONTAINER_NAME grep "\"duration\"" $LOG_DIR/application.json | \
        jq -r 'select(.duration > 5000) |
               "\(.["@timestamp"]) URI: \(.requestUri // .uri) Method: \(.requestMethod // .method) Duration: \(.duration)ms"' | \
        tail -10

        if [ $? -ne 0 ] || [ -z "$(docker exec $CONTAINER_NAME grep "\"duration\"" $LOG_DIR/application.json | jq -r 'select(.duration > 5000)')" ]; then
            echo -e "${GREEN}✅ 느린 요청이 발견되지 않았습니다!${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  애플리케이션 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 보안 이벤트 확인
function check_security_events() {
    echo -e "${PURPLE}🔒 보안 이벤트 로그 (최근 20개):${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/security.json; then
        docker exec $CONTAINER_NAME tail -20 $LOG_DIR/security.json | \
        jq -r '"\(.["@timestamp"]) [\(.event)] \(.message) (IP: \(.clientIp), User: \(.userId // .identifier // "N/A"))"'
    else
        echo -e "${YELLOW}⚠️  보안 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 로그 통계
function log_statistics() {
    echo -e "${CYAN}📊 로그 통계 (최근 1시간):${NC}"
    echo ""

    local one_hour_ago=$(date -u -d '1 hour ago' +%s)000

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/application.json; then
        echo -e "${BLUE}총 요청 수:${NC}"
        local total_requests=$(docker exec $CONTAINER_NAME grep "HTTP Request Started" $LOG_DIR/application.json | \
        jq --argjson since "$one_hour_ago" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l)
        echo "  $total_requests 건"

        echo -e "${RED}에러 수:${NC}"
        local error_count=$(docker exec $CONTAINER_NAME grep "\"log.level\":\"ERROR\"" $LOG_DIR/application.json | \
        jq --argjson since "$one_hour_ago" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l)
        echo "  $error_count 건"

        echo -e "${YELLOW}경고 수:${NC}"
        local warn_count=$(docker exec $CONTAINER_NAME grep "\"log.level\":\"WARN\"" $LOG_DIR/application.json | \
        jq --argjson since "$one_hour_ago" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l)
        echo "  $warn_count 건"

        echo -e "${GREEN}평균 응답시간:${NC}"
        local avg_duration=$(docker exec $CONTAINER_NAME grep "HTTP Request Completed" $LOG_DIR/application.json | \
        jq --argjson since "$one_hour_ago" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since) | .duration' 2>/dev/null | \
        awk '{sum+=$1; count++} END {if(count>0) printf "%.0f", sum/count; else print "0"}')
        echo "  ${avg_duration}ms"

    else
        echo -e "${YELLOW}⚠️  애플리케이션 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 로그 파일 크기 확인
function check_log_sizes() {
    echo -e "${CYAN}💾 로그 파일 크기:${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -d $LOG_DIR; then
        docker exec $CONTAINER_NAME ls -lh $LOG_DIR/ | while read line; do
            if [[ $line == *".json"* ]]; then
                echo -e "${GREEN}$line${NC}"
            else
                echo "$line"
            fi
        done

        echo ""
        echo -e "${BLUE}전체 로그 디렉토리 크기:${NC}"
        docker exec $CONTAINER_NAME du -sh $LOG_DIR/
    else
        echo -e "${RED}❌ 로그 디렉토리가 존재하지 않습니다: $LOG_DIR${NC}"
    fi
}

# 로그 압축 및 정리
function cleanup_logs() {
    echo -e "${YELLOW}🧹 로그 정리 중...${NC}"
    echo ""

    # 7일 이상 된 로그 파일 압축
    echo "7일 이상 된 로그 파일 압축 중..."
    docker exec $CONTAINER_NAME find $LOG_DIR -name "*.json.*" -mtime +7 -exec gzip {} \; 2>/dev/null || true

    # 30일 이상 된 압축 파일 삭제
    echo "30일 이상 된 압축 파일 삭제 중..."
    docker exec $CONTAINER_NAME find $LOG_DIR -name "*.gz" -mtime +30 -delete 2>/dev/null || true

    # 빈 로그 파일 제거
    echo "빈 로그 파일 제거 중..."
    docker exec $CONTAINER_NAME find $LOG_DIR -name "*.json" -size 0 -delete 2>/dev/null || true

    echo -e "${GREEN}✅ 로그 정리 완료${NC}"
    check_log_sizes
}

# 특정 시간대 로그 검색
function search_logs_by_time() {
    echo -n "검색할 시간을 입력하세요 (예: 2024-01-15T10:30): "
    read search_time

    if [ -z "$search_time" ]; then
        echo -e "${RED}❌ 시간이 입력되지 않았습니다.${NC}"
        return 1
    fi

    echo -e "${BLUE}🕐 $search_time 시간대 로그 검색:${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/application.json; then
        docker exec $CONTAINER_NAME grep "$search_time" $LOG_DIR/application.json | \
        jq -r '"\(.["@timestamp"]) [\(.["log.level"])] \(.message)"' | \
        head -20
    else
        echo -e "${YELLOW}⚠️  애플리케이션 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 로그 레벨별 통계
function log_level_stats() {
    echo -e "${CYAN}📈 로그 레벨별 통계 (최근 24시간):${NC}"
    echo ""

    if docker exec $CONTAINER_NAME test -f $LOG_DIR/application.json; then
        local yesterday=$(date -u -d '1 day ago' +%s)000

        echo -e "${GREEN}INFO:${NC}"
        docker exec $CONTAINER_NAME grep "\"log.level\":\"INFO\"" $LOG_DIR/application.json | \
        jq --argjson since "$yesterday" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l

        echo -e "${YELLOW}WARN:${NC}"
        docker exec $CONTAINER_NAME grep "\"log.level\":\"WARN\"" $LOG_DIR/application.json | \
        jq --argjson since "$yesterday" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l

        echo -e "${RED}ERROR:${NC}"
        docker exec $CONTAINER_NAME grep "\"log.level\":\"ERROR\"" $LOG_DIR/application.json | \
        jq --argjson since "$yesterday" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l

        echo -e "${BLUE}DEBUG:${NC}"
        docker exec $CONTAINER_NAME grep "\"log.level\":\"DEBUG\"" $LOG_DIR/application.json | \
        jq --argjson since "$yesterday" 'select(.["@timestamp"] | fromdateiso8601 * 1000 > $since)' 2>/dev/null | wc -l

    else
        echo -e "${YELLOW}⚠️  애플리케이션 로그 파일이 존재하지 않습니다.${NC}"
    fi
}

# 메뉴 출력
function show_menu() {
    clear
    echo -e "${CYAN}🚀 TokTot 로그 관리 도구${NC}"
    echo -e "${CYAN}========================${NC}"
    echo -e "${GREEN}1.${NC} 실시간 로그 확인"
    echo -e "${RED}2.${NC} 에러 로그 확인"
    echo -e "${BLUE}3.${NC} 사용자별 로그 검색"
    echo -e "${YELLOW}4.${NC} 느린 요청 분석"
    echo -e "${PURPLE}5.${NC} 보안 이벤트 확인"
    echo -e "${CYAN}6.${NC} 로그 통계"
    echo -e "${GREEN}7.${NC} 로그 파일 크기 확인"
    echo -e "${YELLOW}8.${NC} 로그 정리"
    echo -e "${BLUE}9.${NC} 시간대별 로그 검색"
    echo -e "${PURPLE}10.${NC} 로그 레벨별 통계"
    echo -e "${RED}0.${NC} 종료"
    echo -e "${CYAN}========================${NC}"
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

# jq 설치 확인
function check_dependencies() {
    if ! command -v jq &> /dev/null; then
        echo -e "${YELLOW}⚠️  jq가 설치되지 않았습니다. JSON 파싱을 위해 설치를 권장합니다.${NC}"
        echo "설치 명령어:"
        echo "  Ubuntu/Debian: apt-get install jq"
        echo "  CentOS/RHEL: yum install jq"
        echo "  macOS: brew install jq"
        echo ""
    fi
}

# 메인 실행 함수
function main() {
    # 초기 검사
    check_dependencies
    check_container

    if [ "$1" ]; then
        case "$1" in
            "tail") tail_logs ;;
            "errors") show_errors ;;
            "user") search_user_logs "$2" ;;
            "slow") analyze_slow_requests ;;
            "security") check_security_events ;;
            "stats") log_statistics ;;
            "size") check_log_sizes ;;
            "cleanup") cleanup_logs ;;
            "time") search_logs_by_time ;;
            "level-stats") log_level_stats ;;
            *) echo -e "${RED}알 수 없는 명령어: $1${NC}" ;;
        esac
        return
    fi

    while true; do
        show_menu
        echo -n "선택하세요: "
        read choice

        case $choice in
            1) tail_logs ;;
            2) show_errors ;;
            3)
                echo -n "사용자 ID 입력: "
                read user_id
                search_user_logs "$user_id"
                ;;
            4) analyze_slow_requests ;;
            5) check_security_events ;;
            6) log_statistics ;;
            7) check_log_sizes ;;
            8) cleanup_logs ;;
            9) search_logs_by_time ;;
            10) log_level_stats ;;
            0) echo -e "${GREEN}종료합니다.${NC}"; break ;;
            *) echo -e "${RED}잘못된 선택입니다.${NC}" ;;
        esac

        echo ""
        echo -e "${CYAN}계속하려면 Enter를 누르세요...${NC}"
        read
    done
}

# 스크립트 실행
main "$@"