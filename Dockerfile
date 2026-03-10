FROM eclipse-temurin:21-jdk-slim

# 필수 라이브러리 업데이트
RUN apt-get update && apt-get install -y libstdc++6

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone \

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]