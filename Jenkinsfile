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

    stage('Setup Config') {
      when { branch 'main' }
      steps {
        configFileProvider([
          configFile(fileId: 'monsoon-prod-yml', targetLocation: 'src/main/resources/application-prod.yml')
        ]) {
          echo 'Injected application-prod.yml for production build'
        }
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
          echo "Building bootJar (skip tests - already run)..."
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

    stage('Update Manifest') {
      when {
        branch 'main' //
      }
      steps {
        script {
          def manifestRepoUrl = "github.com/beyond-team3/final-manifests.git"
          def targetFile = "backend/deployment.yml"
          def imageName = "21monsoon/monsoon-backend"
          def newTag = "${env.APP_VERSION_PREFIX}.${env.BUILD_ID}"

          withCredentials([usernamePassword(credentialsId: 'github-access-token', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {

            sh """
                set +x
                # 기존 폴더 정리
                rm -rf temp-manifests

                # Clone (이때는 비밀번호 사용)
                git clone https://${GIT_USER}:${GIT_PASS}@${manifestRepoUrl} temp-manifests

                cd temp-manifests

                # 로컬 설정 파일(.git/config)에서 비밀번호가 포함된 URL을 즉시 삭제
                git remote set-url origin https://${manifestRepoUrl}

                # 젠킨스 봇 계정 설정
                git config user.email "jenkins-bot@monsoon.com"
                git config user.name "Jenkins-CI-Bot"

                # 파일 수정
                sed -i "s|image: ${imageName}:.*|image: ${imageName}:${newTag}|g" ${targetFile}

                # 커밋 및 푸시
                git add "${targetFile}"

                git diff-index --quiet HEAD || (git commit -m "🚀 [CD] Update ${imageName} to ${newTag} [skip ci]"
                && git push https://${GIT_USER}:${GIT_PASS}@${manifestRepoUrl} main)

                set -x
            """
            }
          }
          echo "✅ Manifest updated in beyond-team3/final-manifests"
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
