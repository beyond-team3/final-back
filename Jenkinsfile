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
  - name: trivy-cli
    image: aquasec/trivy:latest
    command: ['cat']
    tty: true
  - name: argocd-cli
    image: argoproj/argocd:v2.10.1
    command: ['cat']
    tty: true
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
        ARGOCD_CREDENTIAL_ID = 'argocd-admin-login'
        DISCORD_WEBHOOK = credentials('discord-webhook-url')
        IMAGE_NAME = '21monsoon/monsoon-backend'
        APP_VERSION_PREFIX = '0.0'
        FINAL_TAG = ""
        // CI에서는 테스트 프로필 강제
        SPRING_PROFILES_ACTIVE = 'test'
    }

    stages {
        stage('Prepare Tag') {
            steps {
                script {
                    // 태그 생성 로직
                    env.FINAL_TAG = "${env.APP_VERSION_PREFIX}.${env.BRANCH_NAME.replaceAll("/", "-")}.${env.BUILD_NUMBER}.${env.GIT_COMMIT.take(7)}"
                }
            }
        }

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
                        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIAL_ID,
                            usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

                            echo "Building and Pushing Tag: " + env.FINAL_TAG

                            // 도커 빌드 (고유 태그 및 latest)
                            sh "docker build --no-cache -t ${IMAGE_NAME}:${env.FINAL_TAG} ."

                            // 도커 로그인 및 푸시
                            sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                            sh "docker push ${IMAGE_NAME}:${env.FINAL_TAG}"

                            // main 브랜치일 경우에만 latest 푸시
                            if (env.BRANCH_NAME == 'main') {
                                sh "docker tag ${IMAGE_NAME}:${env.FINAL_TAG} ${IMAGE_NAME}:latest"
                                sh "docker push ${IMAGE_NAME}:latest"
                            }

                            sh "docker logout"
                        }
                    }
                }
            }
        }

        stage('Image Scan') {
            steps {
                container('trivy-cli') {
                    // Trivy을 활용한 스캔 (실패 시 빌드 중단 가능)
                    sh "trivy image --severity CRITICAL --exit-code 1 ${IMAGE_NAME}:${env.FINAL_TAG}"
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
                sshagent(credentials: ['github-deploy-key']) {
                    script {
                        def targetBranch = (env.BRANCH_NAME == 'main') ? 'main' : 'dev'

                        echo "Targeting Tag: " + env.FINAL_TAG

                        sh """
                            git clone git@github.com:beyond-team3/final-manifests.git temp-manifests
                            cd temp-manifests
                            git checkout ${targetBranch}
                            sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${env.FINAL_TAG}|g" backend/deployment.yml

                            git config user.email "jenkins-bot@monsoon.com"
                            git config user.name "Jenkins-CI-Bot"
                            git add backend/deployment.yml
                            if ! git diff --cached --quiet; then
                                git commit -m "🚀 [CD] Update to ${env.FINAL_TAG} [skip ci]"
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

        stage('Wait for ArgoCD Sync') {
            steps {
                container('argocd-cli') {
                    script {
                        withCredentials([usernamePassword(credentialsId: env.ARGOCD_CREDENTIAL_ID,
                            usernameVariable: 'ARGO_USER', passwordVariable: 'ARGO_PASS')]) {

                            sh "argocd login [ArgoCD-Server-URL] --username ${ARGO_USER} --password ${ARGO_PASS} --insecure"
                            sh "argocd app wait monsoon-app --timeout 300"

                        }
                        discordSend(
                            webhookURL: env.DISCORD_WEBHOOK,
                            title: "[Backend] 배포 완료!",
                            description: "도메인: https://www.monsoonseed.com\n버전: ${env.FINAL_TAG}",
                            result: 'SUCCESS',
                            color: '#00FF00'
                        )
                    }
                }
            }
        }
    }

    post {
        failure {
            discordSend(
                webhookURL: env.DISCORD_WEBHOOK,
                title: "🔴 [Backend] 빌드 실패",
                description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
                result: 'FAILURE'
            )
        }
    }
}
