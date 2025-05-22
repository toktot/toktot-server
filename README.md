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

# 운영 환경 설정 파일 생성 (필요시)
cp application-prod.yml.template src/main/resources/application-prod.yml
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

### Docker 환경 (권장)
```bash
# 전체 환경 실행 (PostgreSQL + Spring Boot)
docker-compose up -d

# 빌드와 함께 실행
docker-compose up --build -d

# 로그 실시간 확인
docker-compose logs -f

# 특정 서비스 로그 확인
docker-compose logs -f app
docker-compose logs -f postgres
```

### 로컬 개발 환경
```bash
# PostgreSQL이 로컬에 설치되어 있어야 함
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는
./gradlew build
java -jar build/libs/toktot-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 운영 환경
```bash
# JAR 파일 빌드 후 실행
./gradlew build
java -jar build/libs/toktot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
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
```

### 데이터베이스 접속
```bash
# PostgreSQL 컨테이너에 직접 접속
docker-compose exec postgres psql -U toktot_user -d toktot

# 또는 로컬에서 접속 (포트 5432)
psql -h localhost -p 5432 -U toktot_user -d toktot
```

### 애플리케이션 접속
- **웹 애플리케이션**: http://localhost:8080
- **헬스체크**: http://localhost:8080/actuator/health

## 🛡 보안 가이드
### ✅ 포함되는 파일 (Git)
- `application.yml` (공통 설정만)
- `*.template` 파일들
- `docker-compose.yml`
- `Dockerfile`

### ❌ 제외되는 파일 (Git)
- `.env`
- `application-dev.yml`
- `application-prod.yml`
- `logs/` 디렉토리

## 🔍 문제 해결

### Docker 권한 문제 (macOS)
```bash
# 시스템 설정 → 개인정보 보호 및 보안 → 전체 디스크 접근 권한
# 터미널 앱에 권한 부여 후 터미널 재시작
```

### 포트 충돌
```bash
# 이미 사용 중인 포트 확인
lsof -i :8080
lsof -i :5432

# 기존 프로세스 종료 후 재시작
```

### 데이터베이스 연결 실패
```bash
# PostgreSQL 컨테이너 상태 확인
docker-compose logs postgres

# 네트워크 확인
docker-compose exec app ping postgres
```