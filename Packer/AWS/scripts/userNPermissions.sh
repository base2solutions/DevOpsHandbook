#!/bin/bash

# User setup
echo "Don't require tty for sudoers"
sed -i "s/^.*requiretty/#Defaults requiretty/" /etc/sudoers

echo 'Install vagrant SSH key'
yum install -y wget
mkdir -pm 700 /root/.ssh
wget --no-check-certificate https://raw.github.com/mitchellh/vagrant/master/keys/vagrant.pub -O /root/.ssh/authorized_keys
chmod 0600 /root/.ssh/authorized_keys
chown -R root:root /root/.ssh
echo vagrant | passwd --stdin root

echo 'Install vagrant user'
adduser vagrant
echo vagrant | passwd --stdin vagrant
mkdir -pm 700 /home/vagrant/.ssh
wget --no-check-certificate https://raw.github.com/mitchellh/vagrant/master/keys/vagrant.pub -O /home/vagrant/.ssh/authorized_keys
chmod 0600 /home/vagrant/.ssh/authorized_keys
chown -R vagrant:vagrant /home/vagrant/.ssh
echo "vagrant ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/vagrant

# Disable SElinux
echo "Disable SElinux"
if [ -f /etc/selinux/config ]; then
	sed -i "s/enforcing/disabled/" /etc/selinux/config
fi

# Enable password authentication
echo "Enable Password Authentication"
sed -ri '/^PasswordAuthentication (yes|no)/d' /etc/ssh/sshd_config
sed -ri 's/^#PasswordAuthentication (yes|no)/PasswordAuthentication yes/g' /etc/ssh/sshd_config
cat <<EOF >> /etc/ssh/sshd_config

DenyUsers vagrant

EOF

# Fix cloud-init config file
sed -i 's/ssh_pwauth:   0/ssh_pwauth:   1/' /etc/cloud/cloud.cfg

