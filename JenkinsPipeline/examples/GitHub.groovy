#!groovy

/*
  Jenkinsfiles are written in groovy.
  Must have #!groovy at the top of Jenkinsfile.
  This is a multi-line comment in groovy.

  Below is a Jenkins Pipeline + GitHub example.
  This Jenkins Pipeline has 2 Stages: Build and Archive
*/

// This is a single line comment in groovy.



  stage ('Build') {             // Build stage of this Pipeline.
    node {
      checkout scm              // Checks out the GitHub branch that is being built.
      parallel(                 // Parallel blocks allow you to run tasks in parallel. Here we build our app UI and backend simultaneously.
            "Build UI": {
              sh '''#!/bin/bash -l
                    npm install
                    grunt build:all
                 '''                  // Multi-line script syntax uses '''.
              stash includes: 'web/', name: 'build_ui' // Stashing folder created by build_ui.sh to use in later stage.
            },
            "Build App": {
              sh 'echo Building App'
              sh './build_app.sh'    // Running a build_app.sh that's at the repo root.
              stash includes: 'app/', name: 'build_app' //// Stashing folder created by build_app.sh to use in later stage.
             }
        )

    }
  }

  stage ('Archive') {    // Archive stage of this Pipeline.
    node {
      unstash 'build_ui' // Unstashing built code into this node & stage.
      unstash 'build_app'
      zip archive: true, dir: '', glob: '', zipFile: 'my_app_latest.zip'
      // Take unstashed folders, zipping them, and archiving. This will be available on the branch in Jenkins for download.
    }

  }
