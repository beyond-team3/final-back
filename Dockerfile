# 이미 Jenkins가 빌드한 jar를 가져와서 실행만 담당
FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache gcompat

WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# Jenkins workspace에 있는 jar 복사
COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]