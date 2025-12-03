#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

CONTAINER_NAME="toktot-dev-app"
COMPOSE_SERVICE="app"

function docker_compose() {
    (cd "$PROJECT_ROOT" && docker-compose "$@")
}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

function print_header() {
    echo -e "${CYAN}================================${NC}"
    echo -e "${CYAN}  TokTot API 성능 분석 도구${NC}"
    echo -e "${CYAN}================================${NC}"
    echo ""
}

function get_logs() {
    local time_filter="$1"

    if [ -z "$time_filter" ]; then
        docker_compose logs $COMPOSE_SERVICE 2>/dev/null | grep "API_PERFORMANCE"
    else
        docker_compose logs --since="$time_filter" $COMPOSE_SERVICE 2>/dev/null | grep "API_PERFORMANCE"
    fi
}

function list_all_performance_logs() {
    echo -e "${GREEN}📋 모든 API 성능 로그:${NC}"
    echo ""

    local logs=$(get_logs)

    if [ -z "$logs" ]; then
        echo -e "${YELLOW}⚠️  성능 로그가 없습니다.${NC}"
        echo "API를 호출한 후 다시 시도해주세요."
        return
    fi

    echo "$logs" | while IFS= read -r line; do
        if [[ $line =~ API_PERFORMANCE\|([A-Z]+)\|([^|]+)\|([0-9]+)ms\|status:([0-9]+) ]]; then
            local method="${BASH_REMATCH[1]}"
            local uri="${BASH_REMATCH[2]}"
            local duration="${BASH_REMATCH[3]}"
            local status="${BASH_REMATCH[4]}"

            if [ "$status" = "200" ]; then
                echo -e "${GREEN}✓${NC} ${BLUE}${method}${NC} ${uri} - ${duration}ms (${status})"
            else
                echo -e "${RED}✗${NC} ${BLUE}${method}${NC} ${uri} - ${duration}ms (${status})"
            fi
        fi
    done

    echo ""
    local total=$(echo "$logs" | wc -l)
    echo -e "${CYAN}총 ${total}개의 성능 로그${NC}"
}

function analyze_specific_api() {
    echo -e "${GREEN}🔍 특정 API 성능 분석${NC}"
    echo ""
    echo -n "분석할 API 경로를 입력하세요 (예: /v1/reviews/feed): "
    read api_path

    if [ -z "$api_path" ]; then
        echo -e "${RED}❌ API 경로를 입력해주세요.${NC}"
        return
    fi

    local logs=$(get_logs | grep "$api_path")

    if [ -z "$logs" ]; then
        echo -e "${YELLOW}⚠️  해당 API의 로그를 찾을 수 없습니다.${NC}"
        return
    fi

    echo -e "${CYAN}분석 대상:${NC} ${api_path}"
    echo ""

    local durations=()
    local total_duration=0
    local count=0
    local min_duration=999999999
    local max_duration=0

    while IFS= read -r line; do
        if [[ $line =~ API_PERFORMANCE\|[A-Z]+\|[^|]+\|([0-9]+)ms\|status:([0-9]+) ]]; then
            local duration="${BASH_REMATCH[1]}"
            local status="${BASH_REMATCH[2]}"

            durations+=("$duration")
            total_duration=$((total_duration + duration))
            count=$((count + 1))

            if [ "$duration" -lt "$min_duration" ]; then
                min_duration=$duration
            fi

            if [ "$duration" -gt "$max_duration" ]; then
                max_duration=$duration
            fi
        fi
    done <<< "$logs"

    if [ $count -eq 0 ]; then
        echo -e "${YELLOW}⚠️  유효한 성능 데이터가 없습니다.${NC}"
        return
    fi

    local avg_duration=$((total_duration / count))

    echo -e "${BLUE}📊 통계 결과:${NC}"
    echo "  • 측정 횟수: ${count}회"
    echo "  • 평균 응답 시간: ${avg_duration}ms"
    echo "  • 최소 응답 시간: ${min_duration}ms"
    echo "  • 최대 응답 시간: ${max_duration}ms"
    echo ""

    echo -e "${BLUE}📈 모든 측정값:${NC}"
    for i in "${!durations[@]}"; do
        local num=$((i + 1))
        echo "  ${num}. ${durations[$i]}ms"
    done
}

function show_api_statistics() {
    echo -e "${GREEN}📊 전체 API 통계${NC}"
    echo ""

    local logs=$(get_logs)

    if [ -z "$logs" ]; then
        echo -e "${YELLOW}⚠️  성능 로그가 없습니다.${NC}"
        return
    fi

    declare -A api_durations
    declare -A api_counts
    declare -A api_methods

    while IFS= read -r line; do
        if [[ $line =~ API_PERFORMANCE\|([A-Z]+)\|([^|]+)\|([0-9]+)ms\|status:([0-9]+) ]]; then
            local method="${BASH_REMATCH[1]}"
            local uri="${BASH_REMATCH[2]}"
            local duration="${BASH_REMATCH[3]}"
            local key="${method}|${uri}"

            if [ -z "${api_durations[$key]}" ]; then
                api_durations[$key]=0
                api_counts[$key]=0
                api_methods[$key]="$method"
            fi

            api_durations[$key]=$((${api_durations[$key]} + duration))
            api_counts[$key]=$((${api_counts[$key]} + 1))
        fi
    done <<< "$logs"

    echo -e "${CYAN}API별 평균 응답 시간:${NC}"
    echo ""
    printf "%-8s %-50s %10s %8s\n" "Method" "URI" "평균(ms)" "호출수"
    echo "--------------------------------------------------------------------------------"

    for key in "${!api_durations[@]}"; do
        local method="${api_methods[$key]}"
        local uri="${key#*|}"
        local total_duration="${api_durations[$key]}"
        local count="${api_counts[$key]}"
        local avg=$((total_duration / count))

        printf "%-8s %-50s %10d %8d\n" "$method" "$uri" "$avg" "$count"
    done | sort -k3 -rn
}

function find_slowest_apis() {
    echo -e "${GREEN}🐌 가장 느린 API Top 10${NC}"
    echo ""

    local logs=$(get_logs)

    if [ -z "$logs" ]; then
        echo -e "${YELLOW}⚠️  성능 로그가 없습니다.${NC}"
        return
    fi

    echo "$logs" | while IFS= read -r line; do
        if [[ $line =~ API_PERFORMANCE\|([A-Z]+)\|([^|]+)\|([0-9]+)ms\|status:([0-9]+) ]]; then
            local method="${BASH_REMATCH[1]}"
            local uri="${BASH_REMATCH[2]}"
            local duration="${BASH_REMATCH[3]}"
            local status="${BASH_REMATCH[4]}"
            echo "${duration}|${method}|${uri}|${status}"
        fi
    done | sort -t'|' -k1 -rn | head -10 | while IFS='|' read -r duration method uri status; do
        if [ "$duration" -gt 1000 ]; then
            echo -e "${RED}⚠️  ${method} ${uri} - ${duration}ms (${status})${NC}"
        elif [ "$duration" -gt 500 ]; then
            echo -e "${YELLOW}⚠️  ${method} ${uri} - ${duration}ms (${status})${NC}"
        else
            echo -e "${GREEN}✓${NC}   ${method} ${uri} - ${duration}ms (${status})"
        fi
    done
}

function export_to_csv() {
    echo -e "${GREEN}💾 CSV 파일로 내보내기${NC}"
    echo ""

    local output_file="performance_data_$(date +%Y%m%d_%H%M%S).csv"

    echo -n "저장할 파일명 (기본: ${output_file}): "
    read custom_filename

    if [ ! -z "$custom_filename" ]; then
        output_file="$custom_filename"
        if [[ ! "$output_file" =~ \.csv$ ]]; then
            output_file="${output_file}.csv"
        fi
    fi

    local logs=$(get_logs)

    if [ -z "$logs" ]; then
        echo -e "${YELLOW}⚠️  내보낼 데이터가 없습니다.${NC}"
        return
    fi

    echo "Method,URI,Duration(ms),Status,Timestamp" > "$output_file"

    echo "$logs" | while IFS= read -r line; do
        if [[ $line =~ ([0-9]{4}-[0-9]{2}-[0-9]{2}[[:space:]][0-9]{2}:[0-9]{2}:[0-9]{2}).*API_PERFORMANCE\|([A-Z]+)\|([^|]+)\|([0-9]+)ms\|status:([0-9]+) ]]; then
            local timestamp="${BASH_REMATCH[1]}"
            local method="${BASH_REMATCH[2]}"
            local uri="${BASH_REMATCH[3]}"
            local duration="${BASH_REMATCH[4]}"
            local status="${BASH_REMATCH[5]}"
            echo "${method},${uri},${duration},${status},${timestamp}"
        fi
    done >> "$output_file"

    local count=$(tail -n +2 "$output_file" | wc -l)
    echo -e "${GREEN}✓ ${count}개의 데이터를 ${output_file}에 저장했습니다.${NC}"
    echo -e "${CYAN}파일 위치: $(pwd)/${output_file}${NC}"
}

function show_time_range_analysis() {
    echo -e "${GREEN}⏰ 시간대별 성능 분석${NC}"
    echo ""
    echo "분석할 시간 범위를 선택하세요:"
    echo "1. 최근 1시간"
    echo "2. 최근 24시간"
    echo "3. 최근 7일"
    echo "4. 사용자 정의"
    echo ""
    echo -n "선택 (1-4): "
    read time_choice

    local time_filter=""
    case $time_choice in
        1) time_filter="1h" ;;
        2) time_filter="24h" ;;
        3) time_filter="7d" ;;
        4)
            echo -n "시간 범위 입력 (예: 30m, 2h, 3d): "
            read custom_time
            if [ ! -z "$custom_time" ]; then
                time_filter="$custom_time"
            else
                echo -e "${RED}❌ 시간 범위를 입력해주세요.${NC}"
                return
            fi
            ;;
        *)
            echo -e "${RED}❌ 잘못된 선택입니다.${NC}"
            return
            ;;
    esac

    echo ""
    echo -e "${CYAN}분석 기간: 최근 ${time_filter}${NC}"
    echo ""

    local logs=$(get_logs "$time_filter")

    if [ -z "$logs" ]; then
        echo -e "${YELLOW}⚠️  해당 기간의 로그가 없습니다.${NC}"
        return
    fi

    local total_duration=0
    local count=0
    local error_count=0

    while IFS= read -r line; do
        if [[ $line =~ API_PERFORMANCE\|[A-Z]+\|[^|]+\|([0-9]+)ms\|status:([0-9]+) ]]; then
            local duration="${BASH_REMATCH[1]}"
            local status="${BASH_REMATCH[2]}"

            total_duration=$((total_duration + duration))
            count=$((count + 1))

            if [ "$status" != "200" ]; then
                error_count=$((error_count + 1))
            fi
        fi
    done <<< "$logs"

    if [ $count -eq 0 ]; then
        echo -e "${YELLOW}⚠️  유효한 데이터가 없습니다.${NC}"
        return
    fi

    local avg_duration=$((total_duration / count))
    local success_rate=$((100 * (count - error_count) / count))

    echo -e "${BLUE}📊 기간별 통계:${NC}"
    echo "  • 총 요청 수: ${count}회"
    echo "  • 평균 응답 시간: ${avg_duration}ms"
    echo "  • 성공률: ${success_rate}%"
    echo "  • 에러 발생: ${error_count}회"
}

function compare_apis() {
    echo -e "${GREEN}⚖️  API 성능 비교${NC}"
    echo ""
    echo "비교할 API들을 입력하세요 (쉼표로 구분)"
    echo "예: /v1/reviews/feed,/v1/restaurants/search"
    echo ""
    echo -n "API 목록: "
    read api_list

    if [ -z "$api_list" ]; then
        echo -e "${RED}❌ API를 입력해주세요.${NC}"
        return
    fi

    IFS=',' read -ra APIS <<< "$api_list"

    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    printf "%-40s %15s %15s %10s\n" "API" "평균(ms)" "최소(ms)" "최대(ms)"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

    for api in "${APIS[@]}"; do
        api=$(echo "$api" | xargs)

        local logs=$(get_logs | grep "$api")

        if [ -z "$logs" ]; then
            printf "%-40s %15s %15s %10s\n" "$api" "N/A" "N/A" "N/A"
            continue
        fi

        local total_duration=0
        local count=0
        local min_duration=999999999
        local max_duration=0

        while IFS= read -r line; do
            if [[ $line =~ API_PERFORMANCE\|[A-Z]+\|[^|]+\|([0-9]+)ms\|status:([0-9]+) ]]; then
                local duration="${BASH_REMATCH[1]}"

                total_duration=$((total_duration + duration))
                count=$((count + 1))

                if [ "$duration" -lt "$min_duration" ]; then
                    min_duration=$duration
                fi

                if [ "$duration" -gt "$max_duration" ]; then
                    max_duration=$duration
                fi
            fi
        done <<< "$logs"

        if [ $count -gt 0 ]; then
            local avg_duration=$((total_duration / count))
            printf "%-40s %15d %15d %10d\n" "$api" "$avg_duration" "$min_duration" "$max_duration"
        else
            printf "%-40s %15s %15s %10s\n" "$api" "N/A" "N/A" "N/A"
        fi
    done

    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

function show_menu() {
    echo ""
    echo -e "${YELLOW}원하는 기능을 선택하세요:${NC}"
    echo ""
    echo "  1. 📋 모든 성능 로그 보기"
    echo "  2. 🔍 특정 API 성능 분석"
    echo "  3. 📊 전체 API 통계"
    echo "  4. 🐌 가장 느린 API Top 10"
    echo "  5. ⏰ 시간대별 성능 분석"
    echo "  6. ⚖️  API 성능 비교"
    echo "  7. 💾 CSV 파일로 내보내기"
    echo "  0. 🚪 종료"
    echo ""
    echo -n "선택: "
}

function main() {
    cd "$PROJECT_ROOT"

    print_header

    while true; do
        show_menu
        read choice
        echo ""

        case $choice in
            1) list_all_performance_logs ;;
            2) analyze_specific_api ;;
            3) show_api_statistics ;;
            4) find_slowest_apis ;;
            5) show_time_range_analysis ;;
            6) compare_apis ;;
            7) export_to_csv ;;
            0)
                echo -e "${GREEN}👋 분석 도구를 종료합니다.${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ 잘못된 선택입니다. 다시 선택해주세요.${NC}"
                ;;
        esac

        echo ""
        echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    done
}

main