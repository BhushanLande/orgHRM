pipeline {
    agent any

    tools {
        jdk 'JDK 18'              // Match name in Jenkins tool config
        maven 'Maven 3.8.5'
    }

    triggers {
        cron('30 02 * * *')       // Daily at 2:30 AM
    }

    environment {
        MAVEN_OPTS = "-Dmaven.test.failure.ignore=true"
        REPORT_DIR = "target/site/serenity"
        RECIPIENTS = "bhushanlande525@gmail.com"
    }

    stages {
        stage('Version Check') {
            steps {
                bat 'java -version'
                bat 'mvn -v'
            }
        }

        stage('Build Project') {
            steps {
                echo "Building project..."
//                 bat 'mvn clean deploy -Dmaven.test.skip=true'
            }
        }

        stage('Run Tests') {
            steps {
                echo "Running Serenity-Cucumber tests..."
                bat 'mvn clean verify -Pserenity-junit'
            }
        }

        stage('Aggregate Report') {
            steps {
                echo "Generating Serenity aggregate report..."
                bat 'mvn serenity:aggregate'
            }
        }

        stage('Publish Serenity TestNG Report') {
            steps {
                publishHTML(target: [
                            reportName: 'Serenity TestNG Report',
                            reportDir: 'target/site/serenity',
                            reportFiles: 'index.html',
                            verbose: true
                ])
            }
        }
    }

      post {
         success {
            mail to: "${env.RECIPIENTS}",
                 subject: "✅ Build Success - Orange HRM",
                 body: "Build URL: ${env.BUILD_URL}\nTest report: ${env.BUILD_URL}Serenity Test Report"
        }
        failure {
            mail to: "${env.RECIPIENTS}",
                 subject: "❌ Build Failed - Orange HRM",
                 body: "Build URL: ${env.BUILD_URL}\nPlease check the console output."
        }
        always {
            // Optional: if you're using surefire for JUnit XML reports
            junit '**//*  *//* target/surefire-reports *//*  *//*.xml'
        }
    }
}
