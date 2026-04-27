// ═══════════════════════════════════════════════════════════════
//  BALAJI PHOTO FRAMES — Jenkinsfile
//  CI/CD Pipeline: GitHub → Jenkins → GCP Ubuntu
//
//  Jenkins Credentials required (Manage Jenkins → Credentials):
//    - docker-hub-creds      : DockerHub username + password
//    - balaji-env-prod       : Secret file (.env content)
//    - gcp-ssh-key           : SSH private key for GCP VM
//    - SLACK_WEBHOOK         : Slack webhook URL (optional)
// ═══════════════════════════════════════════════════════════════

pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk   'JDK-17'
    }

    environment {
        APP_NAME     = 'balaji-frames'
        APP_VERSION  = '1.0.0'
        DOCKER_IMAGE = "yourdockerhub/${APP_NAME}"
        DOCKER_TAG   = "${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}-${env.BUILD_NUMBER}"
        GCP_VM_USER  = 'ubuntu'
        GCP_VM_IP    = '34.xxx.xxx.xxx'   // ← Your GCP external IP
    }

    parameters {
        choice(name: 'DEPLOY_ENV',
               choices: ['dev', 'staging', 'prod'],
               description: 'Target environment')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false)
        booleanParam(name: 'PUSH_DOCKER', defaultValue: true)
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_MSG    = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    env.GIT_AUTHOR = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                }
                echo "Branch: ${env.BRANCH_NAME} | Commit: ${env.GIT_MSG}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -q'
            }
        }

        stage('Test') {
            when { expression { !params.SKIP_TESTS } }
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests -q'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker Build & Push') {
            when {
                allOf {
                    expression { params.PUSH_DOCKER }
                    branch 'main'
                }
            }
            steps {
                script {
                    // Credentials from Jenkins — NEVER hardcoded
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                            docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                            echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                            docker push ${DOCKER_IMAGE}:latest
                            docker logout
                        """
                    }
                }
            }
        }

        stage('Deploy to GCP') {
            when {
                allOf {
                    branch 'main'
                    expression { params.DEPLOY_ENV == 'prod' }
                }
            }
            steps {
                script {
                    // .env file from Jenkins credentials — contains all secrets
                    withCredentials([
                        file(credentialsId: 'balaji-env-prod', variable: 'ENV_FILE'),
                        sshUserPrivateKey(credentialsId: 'gcp-ssh-key',
                                         keyFileVariable: 'SSH_KEY')
                    ]) {
                        sh """
                            # Copy JAR and .env to GCP VM
                            scp -i \$SSH_KEY -o StrictHostKeyChecking=no \\
                                target/${APP_NAME}-${APP_VERSION}.jar \\
                                ${GCP_VM_USER}@${GCP_VM_IP}:/opt/balaji/

                            scp -i \$SSH_KEY -o StrictHostKeyChecking=no \\
                                \$ENV_FILE \\
                                ${GCP_VM_USER}@${GCP_VM_IP}:/etc/balaji/.env

                            # SSH in and restart app
                            ssh -i \$SSH_KEY -o StrictHostKeyChecking=no \\
                                ${GCP_VM_USER}@${GCP_VM_IP} \\
                                'sudo chmod 600 /etc/balaji/.env && \\
                                 sudo /opt/balaji/deploy.sh'
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ BUILD SUCCESS — ${APP_NAME} deployed to ${params.DEPLOY_ENV}"
        }
        failure {
            echo "❌ BUILD FAILED — check console output"
        }
        always {
            cleanWs()
        }
    }
}
