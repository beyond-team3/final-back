pipeline {
  agent any

  tools {
    jdk 'jdk-21'
  }

  environment {
    DOCKER_CREDENTIAL_ID = 'docker-hub-id'
    DISCORD_WEBHOOK = credentials('discord-webhook-url')
    IMAGE_NAME = '21monsoon/monsoon-backend'
    APP_VERSION_PREFIX = '0.0'

    // CI에서는 테스트 프로필 강제
    SPRING_PROFILES_ACTIVE = 'test'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Unit Test') {
      steps {
        script {
          echo "Running tests with profile=${env.SPRING_PROFILES_ACTIVE} (H2)..."
          sh 'chmod +x ./gradlew'

          // 프로필을 JVM 옵션으로도 한 번 더 강제 (안전장치)
          sh './gradlew clean test -Dspring.profiles.active=test --stacktrace --info'
        }
      }
    }

    stage('Build Jar') {
      steps {
        script {
          echo "Building bootJar (skip tests - already ran)..."
          sh './gradlew bootJar -x test'
        }
      }
    }

    stage('Docker Build & Push') {
      when {
        branch 'main'
      }
      steps {
        script {
          // 운영 배포에서만 prod 설정 주입
          configFileProvider([
            configFile(fileId: 'monsoon-prod-yml', targetLocation: 'src/main/resources/application-prod.yml')
          ]) {
            echo 'Injected application-prod.yml (main only)'
          }

          // 배포는 prod 프로필로 동작해야 하니, 도커 빌드 시점에만 prod로 전환
          // (컨테이너 런타임에서도 ENV로 SPRING_PROFILES_ACTIVE=prod 주는 게 정석)
          def newTag = "${env.APP_VERSION_PREFIX}.${env.BUILD_ID}"
          echo "Building Docker Image: ${IMAGE_NAME}:${newTag}"

          docker.withRegistry('', "${DOCKER_CREDENTIAL_ID}") {
            def customImage = docker.build("${IMAGE_NAME}:${newTag}")
            customImage.push()
            customImage.push('latest')
          }
        }
      }
    }
  }

  post {
    success {
      discordSend(
        webhookURL: "${env.DISCORD_WEBHOOK}",
        title: "🟢[Backend] 빌드 성공",
        description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
        result: 'SUCCESS'
      )
    }
    failure {
      discordSend(
        webhookURL: "${env.DISCORD_WEBHOOK}",
        title: "🔴[Backend] 빌드 실패",
        description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
        result: 'FAILURE'
      )
    }
  }
}
