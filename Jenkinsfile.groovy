pipeline {
    agent any

    environment {
        DOCKERHUB_USERNAME = 'frenzy669'
        APP_NAME = 'jb-devops-final-project'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        IMAGE_NAME = "${DOCKERHUB_USERNAME}/${APP_NAME}"
        REGISTRY_CREDINTIALS = 'dockerhub'
        VALUES_LOCATION = 'helm-chart/values.yaml'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                script {
                    cleanWs()
                }
            }
        }

        stage('Checkout SCM') {
            steps {
                git(credentialsId: 'github',
                        url: 'https://github.com/YossiF93/jb-devops-final-project',
                        branch: 'development'
                )
            }
        }

        stage('Docker Image Build & Push') {
            steps {
                script {
                    dockerImage = docker.build("${IMAGE_NAME}:${BUILD_NUMBER}")
                    withDockerRegistry(credentialsId: "${REGISTRY_CREDINTIALS}") {
                        dockerImage.push("${env.BUILD_NUMBER}")
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Delete Docker Images') {
            steps {
                sh "docker rmi ${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker rmi ${IMAGE_NAME}:latest"
            }
        }

        stage('Update Docker Image Tag') {
            steps {
                sh "sed -i 's/tag: .*/tag: ${IMAGE_TAG}/g' ${VALUES_LOCATION}"
            }
        }

        stage('Push Changes To DEV Branch') {
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'github', gitToolName: 'Default')]) {
                        sh """
                            git config --global user.name "jenkins"
                            git config --global user.email "jenkins@jenkins.com"
                            git add .
                            git commit -m 'Updated image tag by Jenkins'
                            git push --set-upstream origin development
                            """
                    }
                }
            }
        }
    }
}
