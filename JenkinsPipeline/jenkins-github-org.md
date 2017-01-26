# Jenkins Pipeline - GitHub Organization

This document describes how to setup a Jenkins Pipeline GitHub Organization on Jenkins.

This allows Jenkins to scan your GitHub Organization for every repo that contains a `Jenkinsfile`,
or for specific repos that contain `Jenkinsfile`.

Jenkins Plugins you'll need:
* Pipeline plugin
* GitHub Branch Source plugin
* Pipeline Utility Steps
* Git client plugin
* Git plugin
* GitHub related plugins
* Credentials plugin

#### a. Create a Jenkinsfile
* Create a `Jenkinsfile` and add it to the root of your repo.
* Add groovy script and push to GitHub.

#### a. Create GitHub Personal Access Token
* Login to GitHub
* Go to `Personal settings -> Personal access tokens`
* Select `Generate new token`
* Select `repo`, `admin:repo_hook`, `admin:org_hook`
* Copy the token, use in next step

#### b. Add Jenkins Credentials
* [define credentials](https://support.cloudbees.com/hc/en-us/articles/203802500-Injecting-Secrets-into-Jenkins-Build-Jobs):

```
Defining Credentials and Secrets
After installing the credentials plugins, your Jenkins sidebar will contain a new *Credentials* page that can be used to define credentials and secrets. The easiest way to define secrets for use in your build jobs is to:

Click the **Credentials** link in the sidebar
Click on the **Global credentials** domain
Click [**Add Credential**]
```
* Select Kind `Username with password`
* Input your GitHub username
* Input the token as your password
* Give it an id and description
* Hit `ok`


#### c. Create GitHub Organization
* Select `New Item` and choose `GitHub Organization`
* Under `Project Sources - Repository Sources` list your organization as `Owner`
* Under `Scan credentials` select the credentials you just added
* Hit `save`
