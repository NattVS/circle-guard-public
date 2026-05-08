pipeline {
    agent any
    environment {
        SERVICE_NAME = 'circleguard-identity-service'
        DOCKER_IMAGE = "circleguard/${SERVICE_NAME}"
        DOCKER_TAG = "dev-${env.BUILD_ID}"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build & Unit Tests') {
            steps {
                sh './gradlew :${SERVICE_NAME}:clean :${SERVICE_NAME}:test --tests "*Unit*"'
            }
        }
        stage('Integration Tests') {
            steps {
                sh './gradlew :${SERVICE_NAME}:test --tests "*IntegrationTest*"'
            }
        }
        stage('Code Quality (SonarQube)') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh './gradlew :${SERVICE_NAME}:sonar'
                }
            }
        }
        stage('Docker Build') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}", "-f ${SERVICE_NAME}/Dockerfile .")
                }
            }
        }
        stage('Deploy to K8s (Dev)') {
            steps {
                sh """
                kubectl set image deployment/${SERVICE_NAME} \
                ${SERVICE_NAME}=${DOCKER_IMAGE}:${DOCKER_TAG} -n dev-environment
                """
            }
        }
    }
}