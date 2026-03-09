pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: jenkins/inbound-agent:3355.v388858a_47b_33-6-jdk21
  - name: docker-cli
    image: docker:27-cli
    command: ['cat']
    tty: true
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
"""
        }
    }

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
            steps {
                container('docker-cli') {
                    script {
                        // 변수 정의 및 태그 생성
                        def prefix = env.APP_VERSION_PREFIX
                        def cleanBranchName = env.BRANCH_NAME.replaceAll("/", "-")
                        def buildNum = env.BUILD_NUMBER
                        def shortSha = env.GIT_COMMIT.take(7)
                        def newTag = prefix + "." + cleanBranchName + "." + buildNum + "." + shortSha

                        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIAL_ID,
                            usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

                            echo "Building and Pushing Tag: " + newTag

                            // 도커 빌드 (고유 태그 및 latest)
                            sh "docker build --no-cache -t ${IMAGE_NAME}:${newTag} ."

                            // 도커 로그인 및 푸시
                            sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                            sh "docker push ${IMAGE_NAME}:${newTag}"

                            // main 브랜치일 경우에만 latest 푸시
                            if (env.BRANCH_NAME == 'main') {
                                sh "docker tag ${IMAGE_NAME}:${newTag} ${IMAGE_NAME}:latest"
                                sh "docker push ${IMAGE_NAME}:latest"
                            }

                            sh "docker logout"
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
