# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

똑똣(TokTot) — 제주 식당/향토음식 리뷰 서비스의 백엔드. Spring Boot 3.4.5, Java 21, PostgreSQL 15, Redis 7, QueryDSL 5 (jakarta), Lombok.

## 명령어

```bash
./gradlew build                      # 빌드 + 테스트
./gradlew clean build -x test        # CI와 동일한 빌드 (테스트 제외)
./gradlew test --tests "com.toktot.ToktotApplicationTests"   # 단일 테스트 클래스
./gradlew test --tests "com.toktot.SomeTest.methodName"       # 단일 테스트 메서드
./gradlew compileJava                # QueryDSL Q클래스 재생성

docker compose -f docker-compose.local.yml up -d              # 로컬 인프라: postgres(127.0.0.1:15432, DB toktot_local) + redis(127.0.0.1:16379)
docker compose -f docker-compose.local.yml ps                 # 헬스체크 상태 확인 (healthy)
docker compose -f docker-compose.local.yml down               # 인프라 중지 (볼륨 유지)
```

- `@SpringBootTest`는 실제 PostgreSQL/Redis 연결과 `.env`의 환경변수가 필요하다. 기본 프로파일은 DB/Redis 호스트명이 `postgres`, `redis`(compose 서비스명)로 설정되어 있어 compose 네트워크 밖에서는 `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`를 덮어써야 한다. 상세는 아래 "테스트".
- QueryDSL Q클래스는 `src/main/generated/`에 생성되며 gitignore 대상이고 `clean` 시 삭제된다.
- 시드 데이터: `src/main/resources/data/*.sql`.
- 필요한 환경변수 목록은 `.env.template` 참고. `.env`에는 실제 키가 들어 있으므로 읽지 않는다. 테스트 실행 시 셸 환경변수로 불러오는 것(`set -a; . ./.env; set +a`)은 허용하되, 내용을 출력하거나 Read하지 않는다.
- Swagger: `/swagger-ui.html`, `/api-docs`.

### 로컬 실행 (Docker 인프라 + IntelliJ 앱)

- `docker-compose.local.yml`은 postgres, redis만 띄우고 앱은 띄우지 않는다. compose는 루트 `.env`를 자동으로 읽어 `POSTGRES_PASSWORD`, `TOKTOT_PG_PORT`, `TOKTOT_REDIS_PORT`를 쓴다. 로컬 redis에는 비밀번호(`requirepass`)가 설정되어 있지 않다.
- 앱은 IntelliJ Run Configuration `ToktotApplication`으로 실행한다: Active profiles `local`, Modify options → Environment variables → `.env` 파일 경로에 루트 `.env` 지정. 앱에는 `.env`를 읽는 장치(dotenv, `spring.config.import`)가 없어서 이 설정이 없으면 placeholder 해석에 실패해 기동되지 않는다.
- 기본값 없이 필요한 환경변수: `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REST_API_KEY`, `S3_BUCKET_NAME`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `TOUR_API_SERVICE_KEY`. `DB_PASSWORD`는 compose의 `POSTGRES_PASSWORD`와 같아야 한다.
- 환경변수가 yml보다 우선하므로 `.env`에 `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST` 등이 있으면 `application-local.yml`의 localhost 설정을 덮어쓴다.
- 호스트 포트는 `TOKTOT_PG_PORT`, `TOKTOT_REDIS_PORT`로 바꿀 수 있다(compose와 `application-local.yml`이 같은 변수를 쓴다). 6379는 이 맥에서 다른 Redis가 쓰고 있어 기본값을 16379로 둔다.
- `local` 프로파일은 `logging.file.path: logs`로 로그 파일을 작업 디렉터리의 `logs/`(gitignore 대상)에 쓴다.
- compose 프로젝트 이름은 `toktot-local`이라 볼륨은 `toktot-local_postgres_data`, `toktot-local_redis_data`다. 예전 `toktot_postgres_local_data`, `toktot_redis_local_data` 볼륨은 연결되지 않은 채 남아 있다.

### 테스트

- 위치: `src/test/java`, main과 같은 패키지
- 도구: JUnit 5, AssertJ(`assertThat`), Mockito — 모두 `spring-boot-starter-test`에 포함. H2·Testcontainers 의존성은 없다
- 실행 (JDK 21 필요, docker 인프라 healthy 상태):

```bash
set -a; . ./.env; set +a; JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
```

#### 테스트 종류 선택 (가벼운 것부터 고려)

| 종류 | 언제 | 필요한 것 |
|---|---|---|
| 순수 단위 테스트 (스프링 없음) | 계산·변환·enum·설정 파일 값 등 빈 없이 검증 가능한 것 | 없음 |
| Mockito 단위 테스트 (`@ExtendWith(MockitoExtension.class)`) | 서비스 로직 분기, 협력 객체 호출 여부 | 없음 |
| `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` | 리포지토리·QueryDSL 쿼리, 쿼리 수 | docker postgres |
| `@SpringBootTest` | 트랜잭션 경계·롤백, 프록시(`@Async`·`@Transactional` 자기 호출), 스케줄 등록, 빈 간 연동, 동시성 | docker postgres·redis + `.env` |

- `@SpringBootTest`를 쓸 때는 단위 테스트로 안 되는 이유가 있어야 한다
- PostgreSQL 전용 문법(acos 템플릿 등)을 쓰는 쿼리는 실제 postgres로 검증한다. H2로 대체하지 않는다

#### 작성 규칙

- 테스트 메서드명: 한글 `상황_결과()` (예: `로컬_프로파일에서는_cron_작업이_등록되지_않는다()`). `@DisplayName`은 쓰지 않는다
- 본문은 given / when / then 순서로 쓰되 주석 라벨 없이 빈 줄로 구분한다
- 여러 테스트가 공유하는 기대값은 같은 패키지의 `XxxFixture` 클래스(package-private, final)에 둔다 (예: `interceptor/scheduler/SchedulerCronFixture`)
- 외부 API(TourAPI·카카오·공공데이터)를 실제로 호출하는 테스트를 만들지 않는다. 필요하면 mock 또는 WireMock
- 테스트 데이터는 테스트가 직접 만들고 정리한다. `@SpringBootTest`에서 DB를 쓰면 `@Transactional` 롤백 또는 명시적 정리
- 인증 사용자: `JwtAuthFilter`가 principal로 `User` 엔티티를 넣으므로 `@WithMockUser`로는 `@AuthenticationPrincipal User`가 채워지지 않을 것으로 추정된다. 첫 컨트롤러 테스트 때 확인한다

## 아키텍처

패키지 루트: `com.toktot`

- `domain/<기능>/` — 기능별 수직 분할 (user, review, restaurant, localfood, folder, report, block, search, statistics). 엔티티는 패키지 루트에, 그 아래 `controller/ service/ repository/ dto/{request,response}/ type/`. 작은 도메인(`block`)은 하위 패키지 없이 평면 구조.
- `external/<API>/` — 외부 API 연동 (tourapi, kakao, jeju, seogwipo). 각각 `client|service / config(Properties) / dto / exception` 구조이며 `WebClient` 사용.
- `interceptor/scheduler/` — `@Scheduled` 배치 (TourAPI 식당 동기화, 제주시/서귀포시 착한가격업소, 리뷰 관련 Redis 작업). `web/controller/*SchedulerController`(`/api/...`)는 이 배치들을 수동 트리거하는 엔드포인트.
- `interceptor/PerformanceInterceptor` — 모든 요청에 `API_PERFORMANCE|METHOD|URI|Nms|status:N` 로그를 남긴다. `scripts/analyze-performance.sh`가 이 포맷을 파싱하므로 포맷 변경 시 스크립트도 함께 확인.
- `config/` — security(JWT), database(JPA auditing, QueryDSL, Redis), web(인터셉터, async), external(S3, Swagger, WebClient).

### 횡단 관례

- **응답 래퍼**: 모든 API는 `ResponseEntity<ApiResponse<T>>`(`web/dto/ApiResponse`)를 반환한다. `{success, message, errorCode, data}`.
- **에러 처리**: 비즈니스 에러는 `throw new ToktotException(ErrorCode.X[, customMessage])`. `GlobalExceptionHandler`는 `RuntimeException`을 포함한 대부분의 에러를 **HTTP 200 + `success=false`**로 반환한다. 예외는 `NoHandlerFoundException`(404)과 `RuntimeException`이 아닌 `Exception`(500)뿐이다. 새 에러는 `ErrorCode` enum에 추가.
- **인증**: Stateless JWT. `JwtAuthFilter`가 토큰의 userId로 `User` 엔티티를 조회해 principal로 설정하므로 컨트롤러는 `@AuthenticationPrincipal User user`로 받는다 (공개 엔드포인트에서는 null 가능). 공개 URL은 `SecurityConfig.PUBLIC_URLS`(`/v1/auth/**` 등), 그 외는 전부 인증 필요.
- **경로**: 사용자 API는 `/v1/...`, 내부/관리용 스케줄러 트리거는 `/api/...`.
- **로깅**: `@Slf4j`. 구조화 로그(`log.atInfo().setMessage(..).addKeyValue(..).log()`)와 일반 `log.info(...)`가 혼재. 콘솔/파일 모두 ECS 포맷.
- **복잡한 조회**: `XxxRepositoryCustom` + `XxxRepositoryImpl`(QueryDSL) 패턴 (예: `ReviewSearchRepositoryImpl`, `RestaurantSearchRepositoryImpl`).

### 리뷰 작성 흐름

이미지 업로드가 리뷰 생성보다 먼저 일어난다. `ReviewImageController`로 업로드한 이미지는 S3 임시 경로에 저장되고(`ReviewS3StorageService.uploadTempImage`) (userId, restaurantId) 키의 Redis 세션(`ReviewSessionService`, TTL `toktot.review.session.ttl-hours`)에 쌓인다. 이후 `POST /v1/reviews`가 세션 이미지를 검증해 리뷰를 저장하고, `ReviewS3MigrationService`로 임시 이미지를 정식 경로로 옮긴 뒤 세션을 삭제한다.

### Redis 사용처

리뷰 이미지 세션, 이메일 인증 코드, 비밀번호 재설정, 착한가격업소 캐시(`GoodPriceCacheService`), 인기 리뷰(`PopularReviewService`).

## 설정 / 배포

- 프로파일: `application.yml`(공통, 기본 DB `toktot_dev`), `application-local.yml`(로컬, DEBUG 로그·SQL 출력), `application-prod.yml`. `ddl-auto: update` — 엔티티 변경이 곧 스키마 변경이다.
- 배포: `main` 브랜치 push → GitHub Actions(`.github/workflows/deploy-main.yml`)가 빌드(테스트 제외) → Docker Hub 푸시 → EC2에서 `scripts/deploy.sh` 실행. 운영은 `docker-compose.yml`(Nginx + certbot 포함).
- README의 일부 내용은 오래되었다: `application-dev.yml.template`은 존재하지 않고, 배포 브랜치는 `dev`가 아니라 `main`이다.
