pipeline {
    agent any

    tools {
        jdk 'JDK 18'          // Must match the name configured in Jenkins
        maven 'Maven 3.8.5'   // Must match the name in Jenkins tool config
    }

    stages {
        stage('Build & Verify') {
            steps {
                sh 'mvn clean verify serenity:aggregate -Pserenity-junit'
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
        }
    }
}