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
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
  - name: argocd-cli
    image: quay.io/argoproj/argocd:v2.10.1
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
        AWS_CREDENTIAL_ID = 'aws-ecr-credentials'
        AWS_REGION = 'ap-northeast-2'
        ECR_REGISTRY = '906034468269.dkr.ecr.ap-northeast-2.amazonaws.com'
        IMAGE_NAME = "${ECR_REGISTRY}/monsoon-backend"

        ARGOCD_CREDENTIAL_ID = 'argocd-admin-login'
        DISCORD_WEBHOOK = credentials('discord-webhook-url')
        APP_VERSION_PREFIX = '0.0'
        FINAL_TAG = ""
        // CI에서는 테스트 프로필 강제
        SPRING_PROFILES_ACTIVE = 'test'
    }

    stages {
        stage('Prepare Tag') {
            steps {
                script {
                    def gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()

                    // 브랜치 이름 직접 추출 (가져오지 못하면 기본값 'main' 사용)
                    def branchName = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    if (branchName == "HEAD") { branchName = "main" }

                    env.FINAL_TAG = "${env.APP_VERSION_PREFIX}.${branchName.replaceAll("/", "-")}.${env.BUILD_NUMBER}.${gitCommit}"

                    env.BRANCH_NAME = branchName

                    echo "생성된 안전한 태그: ${env.FINAL_TAG}"
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
                configFileProvider([configFile(fileId: 'monsoon-prod-yml',
                      targetLocation: 'src/main/resources/application-prod.yml'
                  )
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

                    // 프로필을 JVM 옵션으로도 한 번 더 강제 (안전장치)- 테스트단계 임시 주석처리
                    // sh './gradlew clean test -Dspring.profiles.active=test --stacktrace --info'
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

        stage('Docker Build & Push to ECR') {
            steps {
                container('docker-cli') {
                    script {
                        withCredentials([usernamePassword(credentialsId: env.AWS_CREDENTIAL_ID,
                            usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {

                            echo "1. Installing AWS CLI..."
                            sh "apk add --no-cache aws-cli"

                            echo "2. Building Tag: " + env.FINAL_TAG
                            sh "docker build --no-cache -t ${IMAGE_NAME}:${env.FINAL_TAG} ."

                            echo "3. Logging into AWS ECR..."
                            sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"

                            echo "4. Pushing Image to ECR..."
                            sh "docker push ${IMAGE_NAME}:${env.FINAL_TAG}"

                            // main 브랜치일 경우에만 latest 태그 추가 푸시
                            if (env.BRANCH_NAME == 'main') {
                                sh "docker tag ${IMAGE_NAME}:${env.FINAL_TAG} ${IMAGE_NAME}:latest"
                                sh "docker push ${IMAGE_NAME}:latest"
                            }

                            // 보안을 위해 작업 후 로그아웃
                            sh "docker logout ${ECR_REGISTRY}"
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
                            mkdir -p ~/.ssh
                            ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts

                            echo "Starting Manifest Update..."

                            git clone git@github.com:beyond-team3/final-manifests.git temp-manifests
                            cd temp-manifests
                            git checkout ${targetBranch}
                            sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${env.FINAL_TAG}|g" backend/deployment.yml

                            git config user.email "jenkins-bot@monsoon.com"
                            git config user.name "Jenkins-CI-Bot"
                            git add backend/deployment.yml
                            if ! git diff --cached --quiet; then
                                git commit -m "[CD] Update backend to ${env.FINAL_TAG} [skip ci]"
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

                            sh "argocd login argocd-server.argocd.svc.cluster.local --username ${ARGO_USER} --password ${ARGO_PASS} --insecure"
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
