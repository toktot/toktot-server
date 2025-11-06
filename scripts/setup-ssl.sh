#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

if [ $# -ne 2 ]; then
    log_error "사용법: $0 <domain> <email>"
    log_error "예시: $0 api.toktot.site admin@toktot.com"
    exit 1
fi

DOMAIN=$1
EMAIL=$2

log_info "🔒 SSL 설정 시작: $DOMAIN"

log_info "📁 필요한 디렉토리 생성..."
mkdir -p nginx/conf.d
mkdir -p certbot/www
mkdir -p certbot/conf

log_info "🛑 기존 컨테이너 중지..."
docker-compose down 2>/dev/null || true

log_info "🚀 HTTP 서버 시작 (SSL 인증용)..."
docker-compose up -d

log_info "⏳ 서버 시작 대기 중..."
sleep 30

log_info "📜 SSL 인증서 발급 시도..."
docker-compose run --rm certbot certonly \
    --webroot \
    --webroot-path /var/www/certbot \
    --email $EMAIL \
    --agree-tos \
    --no-eff-email \
    --force-renewal \
    -d $DOMAIN

if [ ! -d "./certbot/conf/live/$DOMAIN" ]; then
    log_error "인증서 발급 실패! 도메인과 DNS 설정을 확인해주세요."
    exit 1
fi

log_success "인증서 발급 완료!"

log_info "⚙️ SSL 설정 파일 생성..."
cat > nginx/conf.d/ssl.conf << EOF
server {
    listen 443 ssl http2;
    server_name $DOMAIN;

    ssl_certificate /etc/nginx/ssl/live/$DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/live/$DOMAIN/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-SHA256:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!EXPORT:!DES:!MD5:!PSK:!RC4;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    location / {
        proxy_pass http://app:8080;
        include /etc/nginx/conf.d/proxy_params.conf;
    }

    access_log /var/log/nginx/access.log json_combined;
    error_log /var/log/nginx/error.log;
}
EOF

log_info "🔄 HTTP → HTTPS 리다이렉트 설정..."
cat > nginx/conf.d/default.conf << EOF
server {
    listen 80;
    server_name $DOMAIN;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    location / {
        return 301 https://\$server_name\$request_uri;
    }

    access_log /var/log/nginx/access.log json_combined;
    error_log /var/log/nginx/error.log;
}
EOF

log_info "🔍 Docker Compose SSL 볼륨 설정 확인..."
if ! grep -q "certbot/conf/:/etc/nginx/ssl/" docker-compose.yml; then
    log_warning "docker-compose.yml에 SSL 볼륨 설정이 없습니다. 수동으로 추가해주세요:"
    echo "      - ./certbot/conf/:/etc/nginx/ssl/:ro"
fi

log_info "🔄 SSL 설정 적용을 위한 컨테이너 재시작..."
docker-compose down
docker-compose up -d

log_info "⏳ SSL 설정 적용 대기 중..."
sleep 30

log_info "🧪 HTTPS 연결 테스트..."
if curl -k -f https://$DOMAIN/health > /dev/null 2>&1; then
    log_success "✅ HTTPS 설정 완료!"
    log_success "🌐 접속 URL: https://$DOMAIN"
else
    log_warning "⚠️ HTTPS 테스트 실패. 로그를 확인해주세요:"
    echo "docker-compose logs nginx"
fi

log_info "📅 인증서 자동 갱신 설정 안내:"
echo "다음 명령어로 인증서 갱신을 확인할 수 있습니다:"
echo "docker-compose run --rm certbot renew --dry-run"
echo ""
echo "자동 갱신을 위해 crontab에 다음을 추가하세요:"
echo "0 12 * * * cd $(pwd) && docker-compose run --rm certbot renew --quiet"

log_success "🎉 SSL 설정이 완료되었습니다!"