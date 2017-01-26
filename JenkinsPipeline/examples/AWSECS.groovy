#!groovy

/*
  Jenkinsfiles are written in groovy.
  Must have #!groovy at the top of Jenkinsfile.
  This is a multi-line comment in groovy.

  Below is a Jenkins Pipeline + AWS Elastic Container Service example.
*/

// This is a single line comment in groovy.

stage ('Build') {
  parallel(
        "Build UI": {
          node('ui-build-slave') { // AWS ECS node
            checkout scm
            sh '''#!/bin/bash -l
                  npm install
                  grunt build:all
               '''
            stash includes: 'web/', name: 'build_ui'
          }
        },
        "Build App": {
          node('app-build-slave') { // AWS ECS node
            checkout scm
            sh './build_app.sh'
            stash includes: 'app/', name: 'build_app'
          }
        },
        "Stash Deploy Script": {
          node {
            checkout scm
            stash includes: 'deploy_app.sh', name: 'deploy_app'
          }
        }
  )
}

stage ('Deploy') {
  node {
    unstash 'build_ui'
    unstash 'build_app'
    unstash 'deploy_app'
    sh './deploy_app.sh'
  }
}
