---
id: jenkins
title: Jenkins
sidebar_position: 2
---

# Jenkins

Run Selenium Boot tests on Jenkins using a `Jenkinsfile`. The pipeline below checks out the code, runs the tests, publishes JUnit results, and archives the HTML report.

---

## Declarative pipeline

```groovy title="Jenkinsfile"
pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh 'mvn clean test -B'
            }
            post {
                always {
                    junit '**/surefire-reports/TEST-*.xml'
                    archiveArtifacts artifacts: 'target/selenium-boot-report.html',
                                     allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            publishHTML(target: [
                allowMissing         : false,
                alwaysLinkToLastBuild: true,
                keepAll              : true,
                reportDir            : 'target',
                reportFiles          : 'selenium-boot-report.html',
                reportName           : 'Selenium Boot Report'
            ])
        }
    }
}
```

> The `publishHTML` step requires the **HTML Publisher Plugin** installed in Jenkins.

---

## Headless Chrome on Jenkins agents

Add Chrome to the agent and configure headless mode:

```yaml title="selenium-boot.yml"
browser:
  type: chrome
  headless: true
```

If Chrome is not in the `PATH` on your agent, set the binary path:

```yaml
browser:
  type: chrome
  headless: true
  binaryPath: /usr/bin/google-chrome
```

---

## Parallel stages

Run multiple browser or test-group configurations in parallel:

```groovy
stage('Test') {
    parallel {
        stage('Chrome') {
            steps {
                sh 'mvn test -B -Dbrowser.type=chrome'
            }
        }
        stage('Firefox') {
            steps {
                sh 'mvn test -B -Dbrowser.type=firefox'
            }
        }
    }
}
```

---

## Environment variables

Pass configuration values without modifying `selenium-boot.yml`:

```groovy
environment {
    BASE_URL = 'https://staging.example.com'
}

stage('Test') {
    steps {
        sh "mvn test -B -DbaseUrl=${env.BASE_URL}"
    }
}
```

---

## Triggering on SCM changes

```groovy
triggers {
    pollSCM('H/5 * * * *')   // poll every 5 minutes
}
```

Or use a GitHub webhook to trigger the pipeline on push.
