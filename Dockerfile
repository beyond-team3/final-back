FROM eclipse-temurin:21-jdk

# AI 모델 실행을 위한 필수 패키지 설치 (apt-get 사용)
RUN apt-get update && apt-get install -y libstdc++6 && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# AI 라이브러리가 임시 파일을 풀 수 있도록 권한 부여
RUN chmod 777 /tmp

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]