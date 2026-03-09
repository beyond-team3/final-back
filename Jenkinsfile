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
                    // 도커 클라이언트 설치
                    sh '''
                        if ! command -v docker &> /dev/null; then
                            echo "Docker CLI not found. Installing..."
                            # 데비안 기반이므로 apt-get을 사용합니다.
                            apt-get update && apt-get install -y docker.io
                        fi
                    '''

                    // 변수 정의
                    def prefix = env.APP_VERSION_PREFIX

                    def cleanBranchName = env.BRANCH_NAME.replaceAll("/", "-")
                    def buildNum = env.BUILD_NUMBER
                    def shortSha = env.GIT_COMMIT.take(7)


                    // 변수 결합하여 태그 생성
                    def newTag = prefix + "." + cleanBranchName + "." + buildNum + "." + shortSha

                    echo "Build Tag: " + newTag // 태그 확인용

                    docker.withRegistry('', DOCKER_CREDENTIAL_ID) {

                        def imgNameWithTag = IMAGE_NAME + ":" + newTag
                        def customImage = docker.build(imgNameWithTag)
                        customImage.push()

                        if (env.BRANCH_NAME == 'main') {
                            customImage.push('latest')
                        }
                    }
                }
            }
        }

        stage('Update Manifest') {
            when {
                anyOf {
                    branch 'main'
                    branch 'dev'
                }
            }
            steps {
                script {
                    def manifestRepoUrl = "git@github.com:beyond-team3/final-manifests.git"
                    def targetFile = "backend/deployment.yml"
                    def imageName = "21monsoon/monsoon-backend"

                    // 태그 준비
                    def prefix = env.APP_VERSION_PREFIX

                    def branchName = env.BRANCH_NAME.replaceAll("/", "-")
                    def buildNum = env.BUILD_NUMBER
                    def shortSha = env.GIT_COMMIT.take(7)

                    // 문자열 결합 방식으로 고유 태그 생성
                    def newTag = prefix + "." + branchName + "." + buildNum + "." + shortSha
                    def targetBranch = (env.BRANCH_NAME == 'main') ? 'main' : 'dev'

                    echo "Targeting Tag: " + newTag

                    // SSH 실행 및 매니페스트 업데이트
                    sshagent(credentials: ['github-deploy-key']) {
                        sh """
                            echo "Starting Manifest Update for: ${newTag}"

                            rm -rf temp-manifests
                            git clone ${manifestRepoUrl} temp-manifests
                            cd temp-manifests

                            git checkout ${targetBranch} || git checkout -b ${targetBranch}

                            git config user.email "jenkins-bot@monsoon.com"
                            git config user.name "Jenkins-CI-Bot"

                            # sed 명령어에서 변수 구분 명확화
                            sed -i "s|image: ${imageName}:.*|image: ${imageName}:${newTag}|g" ${targetFile}

                            git add ${targetFile}

                            if git diff --cached --quiet; then
                                echo "No changes detected; skip commit/push"
                            else
                                git commit -m "🚀 [CD-${targetBranch.toUpperCase()}] Update ${imageName} to ${newTag} [skip ci]"
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
