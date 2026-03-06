pipeline {
    agent {
        label 'built-in' // Это заставит билд выполняться на мастере
    }

    tools {
        // Имя должно быть таким же, как в Global Tool Configuration
        maven 'maven-3'
    }

    environment {
        // Имя твоего образа
        IMAGE_NAME = "mcp-server"
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Lawrence982/mcp-server-service'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Docker Build') {
            steps {
                script {
                    sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -f deployments/docker/Dockerfile .'
                }
            }
        }
        stage('Cleanup Old Images') {
            steps {
                script {
                    // Оставляем последние 3 образа, удаляем остальные
                    sh """
                        docker images ${IMAGE_NAME} --format '{{.Tag}}' \
                            | grep -E '^[0-9]+\$' \
                            | sort -rn \
                            | tail -n +4 \
                            | xargs -r -I{} docker rmi ${IMAGE_NAME}:{} || true
                    """
                }
            }
        }
        stage('Deploy Services') {
            steps {
                script {
                    // Список чартов
                    def charts = ['mcp-server', 'inspector']

                    charts.each { chartName ->
                    // Формируем доп. параметры только для mcp-server
                    def overrideTag = (chartName == "mcp-server") ? "--set IMAGE.TAG=${IMAGE_TAG}" : ""

                    echo "Deploying ${chartName}..."
                        sh """
                            /var/jenkins_home/helm upgrade --install ${chartName} ./deployments/umbrella-chart/charts/${chartName} \
                            ${overrideTag} \
                            --wait -n default
                        """
                   }
                }
            }
       }
    }
}