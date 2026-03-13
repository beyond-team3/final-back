FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# 소스 복사 및 빌드 (테스트 제외하여 속도 향상)
COPY . .
RUN gradle clean bootJar -x test

# 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 빌드된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# JVM 최적화
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]