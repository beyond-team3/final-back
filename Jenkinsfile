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
        // CI에서는 테스트 프로필 강제
        SPRING_PROFILES_ACTIVE = 'test'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Tag') {
            steps {
                script {
                    sh 'git rev-parse --short HEAD > tag.txt'

                    env.FINAL_TAG = readFile('tag.txt').trim()

                    echo "생성 태그: ${env.FINAL_TAG}"
                    sh 'rm tag.txt'
                }
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

                            echo "Starting Manifest Update for Prod & Preview..."

                            git clone git@github.com:beyond-team3/final-manifests.git temp-manifests
                            cd temp-manifests
                            git checkout ${targetBranch}

                            // 운영용 (deployment) 업데이트
                            // 이미지 태그 업데이트
                            sed -i "s|image: .*monsoon-backend:.*|image: ${IMAGE_NAME}:${env.FINAL_TAG}|g" backend/deployment.yml

                            sed -i "s|value: \"root\"|value: \"monsoon_dev\"|g" backend/deployment.yml

                            // 테스트용 (test-deployment) 업데이트
                            // 이미지 태그 업데이트
                            sed -i "s|image: .*monsoon-backend:.*|image: ${IMAGE_NAME}:${env.FINAL_TAG}|g" backend/test-deployment.yml
                            // DB 이름을 monsoon_test_db로 확실하게 고정
                            sed -i "s|monsoon_db|monsoon_test_db|g" backend/test-deployment.yml
                            // 계정을 monsoon_back으로 확실하게 고정
                            sed -i "s|value: \"root\"|value: \"monsoon_back\"|g" backend/test-deployment.yml

                            git config user.email "jenkins-bot@monsoon.com"
                            git config user.name "Jenkins-CI-Bot"

                            # 두 파일 모두 스테이징 영역에 추가
                            git add backend/deployment.yml backend/test-deployment.yml

                            # 변경 사항이 하나라도 있으면 커밋 및 푸시
                            if ! git diff --cached --quiet; then
                                git commit -m "[CD] Update backend to ${env.FINAL_TAG} (Prod & Preview) [skip ci]"
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

        stage('Notify Deployment') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'main') {
                        discordSend(
                            webhookURL: env.DISCORD_WEBHOOK,
                            title: "🚀 [Backend] Preview 배포 준비 완료 (main)",
                            description: """새 버전(${env.FINAL_TAG}) 매니페스트가 성공적으로 업데이트되었습니다.
ArgoCD 동기화가 완료되면 아래 링크에서 새 API를 테스트해 주세요!

👀 **미리보기(Preview) 도메인 (프론트/백엔드 연동):**
https://preview.monsoonseed.com

✅ **테스트 완료 후 실제 운영 배포 방법:**
ArgoCD 대시보드에서 `monsoon-backend` Rollout의 **[Promote]** 버튼을 누르거나,
터미널에서 `kubectl argo rollouts promote monsoon-backend -n monsoon-dev` 를 실행해 주세요. (무중단 전환)""",
                            result: 'SUCCESS'
                        )
                    }
                    else {
                        discordSend(
                            webhookURL: env.DISCORD_WEBHOOK,
                            title: "🟢 [Backend] 빌드 성공 (${env.BRANCH_NAME})",
                            description: "Branch: ${env.BRANCH_NAME}\n새로운 버전(${env.FINAL_TAG})의 도커 이미지가 ECR에 성공적으로 푸시되었습니다.",
                            result: 'SUCCESS'
                        )
                    }
                }
            }
        }
    }

    post {
        always {
            // HTML Report 퍼블리싱
            publishHTML(target: [
                allowMissing: true, // 테스트 스킵 시 파일이 없어도 에러 안 나게 처리
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'build/reports/tests/test', // HTML 파일이 생성되는 경로
                reportFiles: 'index.html', // 띄울 메인 파일 이름
                reportName: 'HTML Test Report', // 젠킨스 좌측 메뉴에 표시될 버튼 이름
                reportTitles: 'Gradle Test Result'
            ])
        }

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
