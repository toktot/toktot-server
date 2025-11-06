## 🚀 프로젝트 설정

### 1. 프로젝트 클론
```bash
git clone https://github.com/toktot/toktot-server
cd toktot
```

### 2. 환경 설정 파일 생성
프로젝트를 처음 실행하기 전에 다음 템플릿 파일들을 복사하여 실제 설정 파일을 만든다.

```bash
# 환경변수 파일 생성
cp .env.template .env

# 개발 환경 설정 파일 생성
cp application-dev.yml.template src/main/resources/application-dev.yml
```

### 3. 설정 파일 수정

#### `.env` 파일 (프로젝트 루트)
```bash
# 데이터베이스 비밀번호를 안전한 값으로 수정
DB_PASSWORD=your_secure_password_here
POSTGRES_PASSWORD=your_secure_password_here

# 개발/운영 프로파일 선택
SPRING_PROFILES_ACTIVE=docker-dev
```

#### `application-dev.yml` 파일 (src/main/resources/)
```yaml
spring:
  datasource:
    username: your_dev_username
    password: your_dev_password
    url: jdbc:postgresql://localhost:5432/toktot_dev
```

### 4. 파일 권한 설정 (보안)
```bash
# .env 파일을 본인만 읽을 수 있도록 설정
chmod 600 .env
```

## 🏃‍♂️ 실행 방법

### 로컬 개발 환경 (Nginx 없음)
```bash
# PostgreSQL, Redis, Spring Boot만 실행
docker-compose -f docker-compose.local.yml up -d

# 로그 실시간 확인
docker-compose -f docker-compose.local.yml logs -f

# 애플리케이션 접속: http://localhost:8080
```

### Dev 서버 환경 (Nginx + SSL 지원)
```bash
# 전체 환경 실행 (PostgreSQL + Redis + Spring Boot + Nginx)
docker-compose up -d

# 빌드와 함께 실행
docker-compose up --build -d

# 로그 실시간 확인
docker-compose logs -f

# 특정 서비스 로그 확인
docker-compose logs -f app
docker-compose logs -f nginx
docker-compose logs -f postgres

# 애플리케이션 접속: http://your-server-ip
```

### SSL 인증서 설정 (운영 환경)
```bash
# 도메인이 있는 경우 SSL 인증서 자동 설정
chmod +x scripts/setup-ssl.sh
./scripts/setup-ssl.sh your-domain.com your-email@example.com

# 예시
./scripts/setup-ssl.sh api.toktot.com admin@toktot.com
```

## 🔧 개발 도구

### 컨테이너 관리
```bash
# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 중지
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v

# 특정 서비스만 재시작
docker-compose restart app
docker-compose restart nginx
```

### Nginx 관리
```bash
# Nginx 설정 테스트
docker-compose exec nginx nginx -t

# Nginx 재로드 (설정 변경 시)
docker-compose exec nginx nginx -s reload

# Nginx 접근 로그 확인
docker-compose exec nginx tail -f /var/log/nginx/access.log

# Nginx 에러 로그 확인
docker-compose exec nginx tail -f /var/log/nginx/error.log
```

### SSL 인증서 관리
```bash
# 인증서 수동 갱신
docker-compose run --rm certbot renew

# 인증서 상태 확인
docker-compose run --rm certbot certificates

# 인증서 강제 갱신 (테스트용)
docker-compose run --rm certbot renew --force-renewal
```

## 🛡 보안 가이드

### ✅ 포함되는 파일 (Git)
- `application.yml` (공통 설정만)
- `*.template` 파일들
- `docker-compose.yml`
- `docker-compose.local.yml`
- `Dockerfile`
- `nginx/nginx.conf`
- `nginx/conf.d/default.conf`
- `nginx/conf.d/proxy_params.conf`

### ❌ 제외되는 파일 (Git)
- `.env`
- `application-dev.yml`
- `logs/` 디렉토리
- `certbot/conf/` (SSL 인증서)
- `nginx/ssl/` (SSL 관련 파일)
- `nginx/conf.d/ssl.conf` (생성된 SSL 설정)

## 🌐 배포 환경별 설정

### 로컬 개발 환경
- **Docker Compose**: `docker-compose.local.yml`
- **프로파일**: `local`
- **포트**: 8080 (직접 접근)
- **HTTPS**: 불필요

### Dev 서버 환경
- **Docker Compose**: `docker-compose.yml`
- **프로파일**: `dev`
- **포트**: 80 (Nginx), 443 (HTTPS)
- **HTTPS**: Let's Encrypt 자동 인증서
- **CI/CD**: GitHub Actions 자동 배포

## 🔍 문제 해결

### 포트 충돌
```bash
# 이미 사용 중인 포트 확인
lsof -i :8080
lsof -i :80
lsof -i :443
lsof -i :5432

# 기존 프로세스 종료 후 재시작
```

### Nginx 설정 오류
```bash
# 설정 파일 문법 검사
docker-compose exec nginx nginx -t

# 설정 오류 로그 확인
docker-compose logs nginx

# 설정 파일 재로드
docker-compose exec nginx nginx -s reload
```

## 📈 모니터링

### 로그 관리
```bash
# 통합 로그 모니터링 스크립트 실행
chmod +x scripts/log-management.sh
./scripts/log-management.sh

# 또는 개별 서비스 로그 확인
docker-compose logs -f --tail=100 app
docker-compose logs -f --tail=100 nginx
```

## 🚀 배포 프로세스

### 자동 배포 (GitHub Actions)
1. `dev` 브랜치에 푸시
2. GitHub Actions가 자동으로 빌드 및 배포
3. Docker Hub에 이미지 푸시
4. EC2 서버에서 자동 업데이트

## 📞 지원

### 개발 환경 문제
- Spring Boot 로그: `docker-compose logs app`
- 데이터베이스 연결: `docker-compose logs postgres`

### 운영 환경 문제
- Nginx 프록시: `docker-compose logs nginx`
- SSL 인증서: `docker-compose logs certbot`
- 시스템 리소스: `docker stats`