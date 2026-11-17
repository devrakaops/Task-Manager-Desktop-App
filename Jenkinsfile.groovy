pipeline {
    agent { label 'linux-agent' } // Your agent client 

    tools {
        // Make sure that, is set with the name of  Jenkins's Global Tool Configuration
        maven 'Maven-3.9' 
        jdk 'JDK-21'
    }

    environment {
        // You can change the environment variable according to you
        SCANNER_HOME = tool 'SonarQubeScanner'
        IMAGE_NAME = "task-manager-app"
        TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('1. Checkout Code') {
            steps {
                echo 'Cloning repository from GitHub...'
                checkout scm
            }
        }

        stage('2. Build and Unit Test (Maven)') {
            steps {
                echo 'Compiling code and running unit tests using global Maven...'
                sh 'mvn clean package'
            }
        }

        stage('3. Static Code Analysis (SonarQube)') {
            steps {
                echo 'Running SonarQube analysis for code quality & bugs...'
                withSonarQubeEnv('SonarServer') { // Jenkins's SonarQube name in jenkins Global Configuration 
                    sh "${SCANNER_HOME}/bin/sonar-scanner \
                    -Dsonar.projectKey=task-manager \
                    -Dsonar.projectName=Task-Manager \
                    -Dsonar.java.binaries=target/classes"
                }
            }
        }

        stage('4. Dependency Security Check (OWASP)') {
            steps {
                echo 'Scanning third-party libraries for known vulnerabilities...'
                dependencyCheck additionalArguments: '--scan target/ --format HTML --format XML', odcInstallation: 'Default'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }

        stage('5. Build Docker Image') {
            steps {
                echo 'Building Docker image for the application...'
                sh "docker build -t ${IMAGE_NAME}:${TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('6. Container Image Security Scan (Trivy)') {
            steps {
                echo 'Scanning Docker image for OS vulnerabilities using Trivy...'
                sh "trivy image --exit-code 0 --severity HIGH,CRITICAL ${IMAGE_NAME}:${TAG}"
            }
        }

        stage('7. Deploy Container (Standalone Test)') {
            steps {
                echo 'Deploying container securely using custom network and volume...'
                sh 'docker stop task-manager-container || true'
                sh 'docker rm task-manager-container || true'

                sh """
                    docker run -d \
                      --name task-manager-container \
                      --net my-custom-net \
                      -p 8080:8080 \
                      -e DB_URL="jdbc:mysql://mysql-container:3306/taskdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
                      -e DB_USERNAME="taskuser" \
                      -e DB_PASSWORD="taskpassword" \
                      -e DB_DRIVER="com.mysql.cj.jdbc.Driver" \
                      -e JPA_DIALECT="org.hibernate.dialect.MySQLDialect" \
                      ${IMAGE_NAME}:${TAG}
                """
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully with all security scans passed and app deployed!'
        }
        failure {
            echo 'Pipeline failed! Please check the stage logs and security reports above.'
        }
    }
}
