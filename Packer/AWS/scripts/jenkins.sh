#!/bin/bash

# Updates and upgrades
yum -y update && yum -y upgrade

# Install Java JDK
yum -y install java-1.8.0-openjdk-devel

# Install docker
yum -y install curl
curl -fsSL https://get.docker.com/ | sh
usermod -aG docker vagrant
service docker start
chkconfig docker on

# Install nginx and git
yum -y install git nginx
chkconfig nginx on

# Install Jenkins
wget -O /etc/yum.repos.d/jenkins.repo http://pkg.jenkins-ci.org/redhat/jenkins.repo
rpm --import https://jenkins-ci.org/redhat/jenkins-ci.org.key
yum -y install jenkins
chkconfig jenkins on
