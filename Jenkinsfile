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
          // 빌드 번호를 포함한 새 태그 생성 (0.0.${BUILD_ID})
          def newTag = "${env.APP_VERSION_PREFIX}.${env.BUILD_ID}"

          withCredentials([usernamePassword(credentialsId: 'github-access-token', passwordVariable: 'GIT_TOKEN', usernameVariable: 'GIT_USER')]) {

            sh "git clone https://${GIT_USER}:${GIT_TOKEN}@github.com/beyond-team3/final-manifests.git manifest-repo"

            dir('manifest-repo') {
              sh """
                git config user.email "jenkins@guntinue.com"
                git config user.name "Jenkins-CI"

                sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${newTag}|g" backend/deployment.yml

                git add backend/deployment.yml
                git commit -m "Deploy: Update ${IMAGE_NAME} image to ${newTag} [skip ci]"
                git push origin main
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
