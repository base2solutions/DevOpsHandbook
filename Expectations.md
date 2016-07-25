# DevOps Expectation at Base2s

## Purpose
This document is to help you be successful as a DevOps team member to deliver quality code to the customer and set clear expectations.  

## Understanding the environment(s) you need to support

With each new project the first thing the DevOps engineer needs to determine is the type and scope of environments they need to build and support. Typically this is done by looking at the production system (if it exists) and working backwards. The goal is to use toolsets and code to enable the automatic building, provisioning, and deployment of project code into environments that are used for development, QA, and staging/production purposes.

## Typical Team Structure

On a typical project there can be one or more developers, QA engineers, a project manager, and you, the DevOps Engineer.  Your role sits in between these functions and acts as the “glue” to make the team productive and successful.

## DevOps Engineer Responsibilities

### CI/CD Environment Development and Maintenance

You are the focal for the build environment on your project(s). Typically this will involve standing up a Jenkins server, configuring slaves, and ensuring that testable artifacts are properly produced and archived for consumption by the QA team. As the project evolves you will need to work closely with the development team to ensure the build environment(s) continue to meet the needs of the project.

Once the initial build environment is functional, you should look for opportunities to enable automated deployments to development, QA, and depending on the project, pre-production and production environments. The continuous deployment pipeline should run automated tests and report build status to the appropriate channel in Slack as software moves through the pipeline.

### Dev/QA Environment Development and Maintenance

Development and QA environments should be defined and code and easily provisioned as new members join the team and environment requirements change. Typically this will involve the usage of Vagrant and Ansible to define an environment that runs on Virtualbox. Work alongside the dev and QA teams to determine the workflow needs of each group and ensure that sufficient tooling exists to automate any manually intensive operations. The source code for the environments should be checked in with the product/project source.

### Production Environment Development and Maintenance

Certain projects may require that we develop and deliver a production environment to our clients. In this case, the DevOps team is responsible for the architecture, implementation, and ongoing maintenance of production and pre-production environments. Each project is unique, but the DevOps engineer will typically be responsible for ensuring that the environments are built such that they are highly available, reproducible, and right-sized for the product. Typical tasks would include using toolsets to configure logging, heath checks, load balancing, etc.

### Test Automation

Depending on your skillsets and the needs of the project, you may be called upon to implement automated tests. The tests you will write would typically aim to address integration and functional level testing. You should work closely with the QA team to automate BVT (build verification test) type activities that are both repetitive and manually intensive. A popular toolset that has been used successfully on past projects is the Robot Test Framework. Test automation should be wired up to the CI/CD pipeline and the test results should be reported as part of the build process.

### Documentation

An important part of the DevOps role is sufficient documentation such that your end users can understand how to best utilize and manage their environments. This documentation is typically done in the markdown format and checked in to the source repository.

### Standups/Bug Triage

You will actively participate in daily stand up and bug triage meetings as a project team member. During these meetings you should discuss what your goals are for the day, what you accomplished the previous day, and notify the project manager of any issues that are blocking you. At the bug triage portion of the meeting you should pay special attention to watch for bugs that might be a result of an environment configuration. In these cases, you may be responsible for fixing the defect.

### Unblocking other Team Members

A major part of your day to day role will be the supporting of your other team members. Keep an active eye on slack to ensure that nobody is stuck. It is your responsibility to help your teammates effectively use the environments you have developed. Always be on the lookout for opportunities to help improve dev/QA workflows.

### Reporting and Communication

Communication is key in any project. It is important to talk to development, your TPM (technical project manager), QA,  and other DevOps members if you are stuck or blocked.  It is your responsibility to communicate any concerns or delays.  Communicating occurs in sprint planning, stand-ups, or as needed.
