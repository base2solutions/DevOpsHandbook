# Jenkins Pipeline Tips

[Jenkins Pipeline Plugin Wiki](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Plugin)


### Clone a Private Repository into a Node

Here's how to clone and access a private repository from within your Jenkins Pipeline Nodes. We will be creating a new Jenkins Project using Jenkins Pipeline and Jenkins Branch Source plugin.

Required Jenkins Plugins:

* Pipeline plugin
* GitHub Branch Source plugin
* Pipeline Utility Steps
* Git client plugin
* Git plugin
* GitHub related plugins
* Credentials plugin

#### a. Create GitHub Personal Access Token
* login to GitHub
* go to `Personal settings -> Personal access tokens`
* select `Generate new token`
* select `repo`, `admin:repo_hook`, `admin:org_hook`
* copy the token

#### b. Add Jenkins Credentials
* [define credentials](https://support.cloudbees.com/hc/en-us/articles/203802500-Injecting-Secrets-into-Jenkins-Build-Jobs):

```
Defining Credentials and Secrets
After installing the credentials plugins, your Jenkins sidebar will contain a new *Credentials* page that can be used to define credentials and secrets. The easiest way to define secrets for use in your build jobs is to:

Click the **Credentials** link in the sidebar
Click on the **Global credentials** domain
Click [**Add Credential**]
```
* select Kind `Username with password`
* input your GitHub username
* input the key as your password
* give it an id and description
* hit `ok`


#### c. Create GitHub Organization
* Select `New Item` and choose `GitHub Organization`
* Under `Project Sources - Repository Sources` list your organization as `Owner`
* Under `Scan credentials` select the credentials you just added
* hit `save`

It will basically save any repository branch with a Jenkinsfile in a folder. You can customize it more just to search one repo etc.

#### d. Generate Groovy Script
* Select your Org folder
* Select `Pipeline Syntax` from the left column
* Choose Sample Step `checkout: General SCM`
* Choose SCM `Git`
* add your repo URL and credentials generated earlier
* choose which branch to build
* Hit `Generate Groovy`
* Create `Jenkinsfile` at repository root.
* Paste generated groovy into `Jenkinsfile` at the root of your repository and push to GitHub.

```
#!groovy
node('node_1') {
  checkout([$class: 'GitSCM', branches: [[name: '*/feature/1']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'your-cred-name', url: 'https://github.com/org-name/repo-name.git']]])
  sh '''#!/bin/bash -l
  echo $0
  ./build_stuff.sh
  echo time to archive
  '''
}
```

#### e. Build
* Select your Org folder from the main jobs page
* Select your repo
* Select which Branch
* Build!

### Jenkins Environment Variables

Jenkins has a few environment variables available at build time. Here's a [noncomprehensive list](https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project)

To view available environment variables, create a test node like so in your `Jenkinsfile`:

```
node {
  sh 'env | sort'
}
```

`sh 'env | sort' ` lists environment variables available in that node

### Git Clone Working Branch
To git clone and checkout to your working branch, set the `env.BRANCH_NAME` to a variable.

Use the variable name when indicating the branch you want checked out.
Check the build's console output to view all available variables.

Example:
```
node {
  branch = env.BRANCH_NAME // setting the env.BRANCH_NAME to a variable
  checkout([
    $class: 'GitSCM',
    branches: [[name: branch]], // using the variable instead of specifying a specific branch name
    doGenerateSubmoduleConfigurations: false,
    extensions: [],
    submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'your-cred-name', url: 'https://github.com/org-name/repo-name.git']]
  ])
```
