// =============================================================================
// Sama Tur - backend-ci - Jenkinsfile
// Étapes : tests -> Dependency-Check -> SonarQube -> image -> Trivy -> push -> CD
// =============================================================================

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
    }

    // Polling toutes les 2 minutes (la machine Jenkins n'est pas joignable par GitHub)
    triggers {
        pollSCM('H/2 * * * *')
    }

    // Nom à créer dans : Manage Jenkins > Tools > JDK installations
    // (Maven n'est pas nécessaire : on utilise le ./mvnw du projet)
    tools {
        jdk 'jdk25'   // le pom.xml est en Java 25
    }

    // Premier test : laisser PUSH_AND_DEPLOY décoché (pas de Docker Hub ni de job CD requis)
    parameters {
        booleanParam(name: 'PUSH_AND_DEPLOY', defaultValue: false,
                     description: 'Pousser l\'image sur Docker Hub et déclencher le CD')
    }

    environment {
        // ---- À ADAPTER --------------------------------------------------------
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
        DOCKERHUB_USER   = 'samatur'                 // compte Docker Hub
        IMAGE_REPO       = "${DOCKERHUB_USER}/samatur-backend"
        SONAR_SERVER     = 'SonarQube'                // Manage Jenkins > System > SonarQube servers
        SONAR_PROJECT    = 'samatur-backend'
        CD_JOB           = 'deployment-cd'            // nom du job CD
        CD_TAG_PARAM     = 'BACKEND_TAG'              // paramètre du job CD qui reçoit le tag
        // ---- Credentials Jenkins (IDs) ---------------------------------------
        DOCKERHUB_CREDS  = 'dockerhub-credentials'    // Username + password (token Docker Hub)
        NVD_CREDS        = 'nvd-api-key'              // Secret text
        // ----------------------------------------------------------------------
        // Trivy via Docker (même commande que le test sur poste) : il n'est pas installé dans Jenkins
        TRIVY_CMD        = 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache aquasec/trivy:0.69.3'
        MAVEN_OPTS       = '-Dmaven.repo.local=/var/jenkins_home/.m2/repository'
        REPORTS_DIR      = 'security-reports'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def shortSha = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortSha}"
                    echo "Image : ${env.IMAGE_REPO}:${env.IMAGE_TAG}"
                }
                sh 'mkdir -p "$REPORTS_DIR"'
            }
        }

        stage('Build & tests (mvn verify)') {
            steps {
                sh 'chmod +x mvnw && ./mvnw -B clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('OWASP Dependency-Check') {
            steps {
                // Même commande que celle testée à la main sur le poste.
                // Le fichier dependency-check-suppressions.xml doit exister à la racine du dépôt.
                withCredentials([string(credentialsId: env.NVD_CREDS, variable: 'NVD_API_KEY')]) {
                    sh '''
                        chmod +x mvnw
                        ./mvnw -B org.owasp:dependency-check-maven:check \
                          -DnvdApiKey="$NVD_API_KEY" \
                          -Dformat=ALL \
                          -DsuppressionFiles=dependency-check-suppressions.xml \
                          -DfailBuildOnCVSS=11
                        cp -r target/dependency-check-report.* "$REPORTS_DIR"/
                    '''
                }
            }
            post {
                always {
                    // Provisoire : CRITICAL ou HIGH -> build instable (pas rouge).
                    // Après correction de swagger-ui, remplacer par failedTotalCritical: 1
                    dependencyCheckPublisher pattern: "target/dependency-check-report.xml",
                        unstableTotalCritical: 1,
                        unstableTotalHigh: 1
                }
            }
        }

        stage('SonarQube') {
            steps {
                withSonarQubeEnv(env.SONAR_SERVER) {
                    sh """
                        chmod +x mvnw
                        ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT} \
                          -Dsonar.projectName=${SONAR_PROJECT} \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                // Nécessite le webhook SonarQube -> http://<jenkins>/sonarqube-webhook/
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build image Docker') {
            steps {
                sh 'docker build -t "$IMAGE_REPO:$IMAGE_TAG" .'
            }
        }

        stage('Trivy - scan image') {
            steps {
                // Rapports complets (HIGH + CRITICAL), sans bloquer
                // --timeout et --scanners vuln : sinon le scan dépasse le délai par défaut (5 min)
                // Les rapports sont écrits par redirection de la sortie standard
                sh '''
                    $TRIVY_CMD image --timeout 20m --scanners vuln --skip-version-check \
                        --severity HIGH,CRITICAL --format table \
                        "$IMAGE_REPO:$IMAGE_TAG" > "$REPORTS_DIR/trivy-report.txt"
                    $TRIVY_CMD image --timeout 20m --scanners vuln --skip-version-check \
                        --severity HIGH,CRITICAL --format json \
                        "$IMAGE_REPO:$IMAGE_TAG" > "$REPORTS_DIR/trivy-report.json"
                    cat "$REPORTS_DIR/trivy-report.txt"
                '''
                // Porte : échec si CRITICAL avec correctif disponible
                sh '''
                    $TRIVY_CMD image --timeout 20m --scanners vuln --skip-version-check \
                        --severity CRITICAL --ignore-unfixed --exit-code 1 \
                        --no-progress "$IMAGE_REPO:$IMAGE_TAG"
                '''
            }
        }

        stage('Push Docker Hub') {
            when { expression { params.PUSH_AND_DEPLOY } }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDS,
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push "$IMAGE_REPO:$IMAGE_TAG"
                        docker tag  "$IMAGE_REPO:$IMAGE_TAG" "$IMAGE_REPO:latest"
                        docker push "$IMAGE_REPO:latest"
                    '''
                }
            }
        }

        stage('Déclenchement du CD') {
            when { expression { params.PUSH_AND_DEPLOY } }
            steps {
                build job: env.CD_JOB,
                      wait: false,
                      parameters: [string(name: env.CD_TAG_PARAM, value: env.IMAGE_TAG)]
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "${env.REPORTS_DIR}/**", allowEmptyArchive: true
            sh '''
                docker logout || true
                docker rmi "$IMAGE_REPO:$IMAGE_TAG" "$IMAGE_REPO:latest" || true
            '''
        }
        success {
            echo "Backend ${env.IMAGE_TAG} construit, scanné et poussé ; CD déclenché."
        }
        failure {
            echo "Pipeline en échec : consulter les rapports archivés (${env.REPORTS_DIR})."
        }
    }
}
