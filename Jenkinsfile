pipeline {
    agent any
    // Install jenkins(https://www.jenkins.io/download/) and add tools in manage jenkins and update with path
    // Add required creds for execution
    tools {
        jdk 'JDK 18'              // Match name in Jenkins tool config and path should be of server
        maven 'Maven 3.8.5'       // Match name in Jenkins tool config and path should be of server
    }

    /* triggers {
        cron('00 01 * * *')       // Daily at 1:00 AM
    } */

    parameters {
        string(name: 'TAGS', defaultValue: '@Smoke', description: 'Cucumber tags to execute')
    }

    environment {
        RECIPIENTS = "bhushanlande525@gmail.com"
    }

    stages {
        stage('Version Check') {
            steps {
               // user sh for linux or mac (bat for windows)
                bat 'java -version'
                bat 'mvn -v'
            }
        }

        stage('Run Tests') {
            steps {
                echo "Running Serenity-Cucumber tests..."
//                 bat 'mvn clean verify -Pserenity-junit'  // user sh for linux or mac (bat for windows)
                bat 'mvn clean verify -Pserenity-junit -Dcucumber.filter.tags="@${parameters.TAGS}"
            }
        }

        stage('Aggregate Report') {
            steps {
                echo "Generating Serenity aggregate report..."
                bat 'mvn serenity:aggregate'             // user sh for linux or mac (bat for windows)
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

        stage('Publish Reports') {
            steps {
               archiveArtifacts allowEmptyArchive: true, artifacts: 'target/site/serenity/index.html', followSymlinks: false
            }
        }
    }

      /* post {
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
            junit '**//*  *//*  *//*  *//* target/surefire-reports *//*  *//*  *//*  *//*.xml'
        }
    } */
}
