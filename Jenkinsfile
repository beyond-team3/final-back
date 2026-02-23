pipeline {
	agent any

	tools {
		jdk 'jdk-21'
	}

	environment {
		DOCKER_CREDENTIAL_ID = 'docker-hub-id'
		DISCORD_WEBHOOK = credentials('discord-webhook-url')
		IMAGE_NAME = '21monsoon/monsoon-backend'

		//version
		APP_VERSION_PREFIX = '0.0'
	}

	stages {
		stage('Checkout') {
			steps {
				checkout scm
			}
		}

		stage('Setup Config') {
            steps {
                configFileProvider([configFile(fileId: 'monsoon-prod-yml', targetLocation: 'src/main/resources/application-prod.yml')]) {
                    echo 'Successfully injected application-prod.yml'
                }
            }
        }

		stage('Unit Test & Build') {
			steps {
				script {
					echo 'Building and Testing with H2...'
					sh 'chmod +x ./gradlew'
					// prod 설정을 포함하여 빌드하도록 설정
                    sh './gradlew clean build -Pprofile=prod'
				}
			}
		}

		stage('Docker Build & Push') {
			// main 브랜치일 때만 배포
			when {
				branch 'main'
			}
			steps {
				script {
					// 0.0 + . + 빌드번호(1, 2, 3...) 조합
					def newTag = "${env.APP_VERSION_PREFIX}.${env.BUILD_ID}"

					echo "Building Docker Image: ${IMAGE_NAME}:${newTag}"

					docker.withRegistry('', "${DOCKER_CREDENTIAL_ID}") {
						def customImage = docker.build("${IMAGE_NAME}:${newTag}")

						// 버전 태그와 latest 태그 둘 다 푸시
						customImage.push()          // 0.0.x 푸시
						customImage.push('latest')  // latest 업데이트
					}
				}
			}
		}
	}

	post {
		success {
			discordSend (webhookURL: "${env.DISCORD_WEBHOOK}",
				title: "🟢[Backend] 빌드 성공",
				description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
				result: 'SUCCESS')
		}
		failure {
			discordSend (webhookURL: "${env.DISCORD_WEBHOOK}",
				title: "🔴[Backend] 빌드 실패",
				description: "Branch: ${env.BRANCH_NAME}\nBuild: #${env.BUILD_ID}",
				result: 'FAILURE')
		}
	}
}