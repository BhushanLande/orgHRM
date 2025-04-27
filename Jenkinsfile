pipeline {
    agent any

    tools {
        jdk 'JDK 18'              // Must match the JDK name in Jenkins tools
        maven 'Maven 3.8.5'       // Must match the Maven name in Jenkins tools
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
                sh 'java --version'
                sh 'mvn -v'
            }
        }

        stage('Build Project') {
            steps {
                echo "Building project..."
                sh 'mvn -s $MAVEN_HOME/conf/settings.xml clean deploy -Dmaven.test.skip=true'
            }
        }

        stage('Run Tests') {
            steps {
                echo "Running Serenity-Cucumber tests..."
                sh 'mvn -s $MAVEN_HOME/conf/settings.xml verify -Pserenity-cucumber'
            }
        }

        stage('Aggregate Report') {
            steps {
                echo "Generating aggregate Serenity report..."
                sh 'mvn -s $MAVEN_HOME/conf/settings.xml verify -Pserenity-cucumber serenity:aggregate'
            }
        }

        stage('Publish Reports') {
            steps {
                publishHTML([
                    reportDir: "${REPORT_DIR}",
                    reportFiles: 'index.html',
                    reportName: 'Serenity Test Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
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
            junit '**/target/surefire-reports/*.xml'
        }
    }
}
