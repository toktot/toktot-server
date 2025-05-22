# 멀티 스테이지 빌드를 사용하여 이미지 크기 최적화
FROM eclipse-temurin:21-jdk as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper와 빌드 파일들 먼저 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐싱을 위해 소스코드 복사 전에 수행)
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사
COPY src src

# 애플리케이션 빌드 (테스트 제외)
RUN ./gradlew clean bootJar -x test --no-daemon

# 런타임 스테이지
FROM eclipse-temurin:21-jre

# 한국 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 헬스체크용 curl 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 애플리케이션 실행을 위한 사용자 생성 (보안)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 파일 권한 설정
RUN chown appuser:appuser app.jar

# 사용자 변경
USER appuser

# 포트 노출
EXPOSE 8080

# 힙 메모리 설정 및 애플리케이션 실행
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]