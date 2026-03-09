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
            when {
                branch 'main'
            }
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
                anyOf{
                branch 'main'
                branch 'dev'
                }
            }
            steps {
                script {
                    def newTag = "${env.APP_VERSION_PREFIX}.${env.BUILD_ID}"
                    echo "Building Docker Image: ${IMAGE_NAME}:${newTag}"

                    docker.withRegistry('', "${DOCKER_CREDENTIAL_ID}") {
                    def customImage = docker.build("${IMAGE_NAME}:${newTag}")
                    customImage.push()

                    // main 브랜치일 때만 latest 태그 부여
                    if (env.BRANCH_NAME == 'main') {
                        customImage.push('latest')
                    }
                }
            }
        }

        stage('Update Manifest') {
            when {
                anyOf{
                    branch 'main'
                    branch 'dev'
                }
            }
            steps {
                script {
                    def manifestRepoUrl = "git@github.com:beyond-team3/final-manifests.git"
                    def targetFile = "backend/deployment.yml"
                    def imageName = "21monsoon/monsoon-backend"
                    def newTag = "${env.APP_VERSION_PREFIX}.${env.BUILD_ID}"

                    def targetBranch = env.BRANCH_NAME == 'main' ? 'main' : 'dev'

                    // SSH 자격 증명 ID 사용
                    sshagent(credentials: ['github-deploy-key']) {
                        sh """
                            rm -rf temp-manifests
                            git clone ${manifestRepoUrl} temp-manifests
                            cd temp-manifests

                            # 브랜치 전환
                            git checkout ${targetBranch} || git checkout -b ${targetBranch}

                            git config user.email "jenkins-bot@monsoon.com"
                            git config user.name "Jenkins-CI-Bot"

                            # 이미지 태그 업데이트
                            sed -i "s|image: ${imageName}:.*|image: ${imageName}:${newTag}|g" ${targetFile}

                            # 변경 사항 확인 및 rebase push
                            git add ${targetFile}

                            if git diff --cached --quiet; then
                                echo "No changes detected; skip commit/push"
                            else
                                git commit -m "🚀 [CD-${targetBranch.toUpperCase()}] Update ${imageName} to ${newTag} [skip ci]"

                            # 동시 푸시 충돌 방지
                            git pull --rebase origin ${targetBranch}
                            git push origin ${targetBranch}
                            fi
                        """
                    }
                }
            }
            post {
                always {
                    sh 'rm -rf temp-manifests'
                }
            }
        }
    }

    post {
        success {
            discordSend(
                webhookURL: "${env.DISCORD_WEBHOOK}",
                title: "🟢 [Backend] 빌드 성공",
                description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
                result: 'SUCCESS'
            )
        }
        failure {
            discordSend(
                webhookURL: "${env.DISCORD_WEBHOOK}",
                title: "🔴 [Backend] 빌드 실패",
                description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
                result: 'FAILURE'
            )
        }
    }
}
