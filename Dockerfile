FROM openjdk:21-jdk-slim

# 필수 라이브러리 설치
RUN apt-get update && apt-get install -y libstdc++6

# 기존 설정 그대로 유지
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# ENTRYPOINT 설정
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]