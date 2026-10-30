pipeline {
  agent {
    dockerfile {
      filename 'ci/jenkins/Dockerfile'
      reuseNode true
      args '--shm-size=2g'
    }
  }

  options {
    disableConcurrentBuilds()
    timeout(time: 30, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('Backend unit and integration tests') {
      steps {
        sh 'mvn -B -ntp test'
      }
    }

    stage('Selenium UI regression') {
      steps {
        dir('automation-selenium') {
          sh 'mvn -B -ntp test -Pregression'
        }
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml, automation-selenium/target/surefire-reports/*.xml'
      archiveArtifacts allowEmptyArchive: true, artifacts: 'target/surefire-reports/**, automation-selenium/target/surefire-reports/**, automation-selenium/target/artifacts/**, automation-selenium/target/application.log'
    }
  }
}
