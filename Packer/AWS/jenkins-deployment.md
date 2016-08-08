# (AWS) Jenkins Deployment

<br />
### Deploy Jenkins via Packer:  
#### Packer Build  
 * Install [Packer](https://www.packer.io/downloads.html)  
 * Clone DevOps repository:  
 >     git clone path/to/github/devops/repo
 * Change directory to the location of the packer template:  
 * Start Packer Build process:  
 >     packer build path/to/json/template  
 * Once the build process has been completed, sign into Amazon AWS Console> Click on Services and click EC2  
 * On EC2:  
   * Under Images, click AMIs  
   * Select the previously created Jenkins AMI and click Launch > launch the AMI to create the Jenkins instance.  

#### Packer Template  
 * Basic layout of a packer template is as follow:  
   * Builder: a set of components that create the initial base image  
   >     Example:  
   >         "builder": [  
   >             {"type": "virtualbox-ovf",  
   >              "source_path": "virtualbox/box.ovf",  
   >              "ssh_username": "vagrant",  
   >              "ssh_password": "vagrant",  
   >              "shutdown_command": "echo 'packer' | sudo -S shutdown -P now"  
   >             }]  
   * Provisioners: plugins that can be attached to the initial base image to bring it to the desired state  
   >     Example:  
   >         "provisioners": [  
   >             {"type": "shell",  
   >              "inline": [  
   >              "sudo mkdir /tm/vboxguest",  
   >              "cd /tmp/vboxguest"  
   >             }]
   * Post-Processors: take provisioned image and convert it to the final usable artifact (vagrant box file)  
   >     Example:  
   >         "post-processors": [  
   >             {"type": "vagrant",  
   >              "output": "dummy.box"  
   >             }],  
 * For full list of available builders/provisioner/post-provisioner, check out [Packer Documentations](https://www.packer.io/docs). If what you are looking for is not found, it is likely available via Packer Plugins  
 * There are two types of variables in Packer:  
   * Global/built-in variables:  
   * User variables
 * The followings are examples of Global/built-in variables:  
   * build_name - The name of the build being run.  
   * build_type - The type of the builder being used currently.  
   * isotime [FORMAT] - UTC time, which can be formatted.  
   * lower - Lowercases the string.  
   * pwd - The working directory while executing Packer.  
   * template_dir - The directory to the template for the build.  
   * timestamp - The current Unix timestamp in UTC.  
   * uuid - Returns a random UUID.  
   * upper - Uppercases the string.  
     * Global variables could be called with {{ variable_name }}
 * To use user variables in a Packer template, you have to define them first in the `variables` section:  
 >     Example:  
 >         "variables": {  
 >             "ssh_username": "root",  
 >             "ssh_password": "root"
 >         }
 * Once you define your variables in the `variables` section, you could then call them as follow with "{{user 'name_of_your_variables`}}"  
 >     Example:  
 >         "variables": {  
 >         "my_username": "root",  
 >         "my_password": "root"
 >         },
 >         "builders": [
 >         {
 >             "type": "virtualbox-iso",
 >             "ssh_username": "{{user `my_username`}}",
 >             "ssh_password": "{{user `my_password`}}"
 >         }]

<br/>
### Configure Jenkins:  
#### Route 53  
 * On AWS, click on Services > click Route 53 > click on Hosted zones  
 * In Hosted zones:  
   * Click Create Hosted Zone and enter the following:  
     * Domain name: jenkins/domain/name  
     * Comment: some/comment/here  
     * Type: Public Hosted Zone  
     * Click Create  
   * In Hosted zones, select the previously created public zone > click Create Record Set  
     * In Create Record Set pane, enter:  
     * Name: jenkins/domain/name  
     * Value: enter the public ip of the newly created Jenkins instance  
     * Click Create  
   * Click Create Hosted Zone again and enter the following:  
     * Domain name: jenkins/domain/name  
     * Comment: some/comment/here   
     * Type: Private Hosted Zone for Amazon VPC  
     * VPC ID: choose the Amazon VPC that you want to associated with the hosted zone  
     * Click Create  
   * In Hosted zones, select the previously created private zone > click Create Record Set  
     * In Create Record Set pane, enter:  
     * Name: jenkins/domain/name  
     * Value: enter the private ip of the newly created Jenkins instance  
     * Click Create  
 * Public Hosted Zone will provide dns name resolution for your Jenkins when it is accessed via the internet. Private Hosted Zone will provide dns name resolutions for your Jenkins when it is accessed by other servers within the same Amazon VPC.  

#### Nginx
 * SSH into the newly created Jenkins instance  
 * Navigate to /etc/nginx/nginx.conf > Uncomment out the following line:
 >     include /etc/nginx/conf.d/*.conf
 
#### Add letsencrypt certs  
 * Stop Nginx:  
 >     sudo service nginx stop  
 * Generate letsencrypt certificate:  
 >     sudo wget https://dl.eff.org/certbot-auto  
 >     sudo chmod a+x certbot-auto  
 >     sudo ./certbot-auto certonly --standalone -d jenkins/domain/name  

#### Set Jenkins SSL certs in Jenkins config file:  
 * modify /etc/nginx/conf.d/jenkins.conf file as follow:  
   * Add:  
   >     ssl_certificate           /etc/letsencrypt/live/jenkins_domain_name/fullchain.pem;  
   >     ssl_certificate_key       /etc/letsencrypt/live/jenkins_domain_name/privkey.pem;  
   >     ssl on;  
   * Comment out:  
   >     #  ssl_certificate           /etc/nginx/cert.crt;  
   >     #  ssl_certificate_key       /etc/nginx/cert.key;  
   >     #  ssl_dhparam               /etc/nginx/ssl/dh2048.pem;  
 * Start Nginx:  
 >     sudo service nginx start  

#### Jenkins First Step   
 * Using a web browser, navigate to jenkins/domain/name  
 * Log in using the password created in `/var/lib/jenkins/secrets/initialAdminPassword`  
 * Select to install most recommended plugins  
 * Lastly, create a local admin account  

#### Manage Jenkins Plugins  
 * Login to Jenkins > click Manage Jenkins > click on Manage Plugins  
 * Click on Available tab > then select:  
 >     Amazon EC2 Container Service plugin  
 >     Amazon EC2 Container Service plugin with autoscaling capabilities  
 >     Amazon EC2 plugin  
 >     Amazon ECR plugin  
 >     GitHub Authentication plugin  
 >     GitHub Pull Request Builder  
 * Click `Download now and install after restart` > the installation will start   
 * Afterward, tick `Restart Jenkins when installation is complete and no jobs are running`.  

#### Configure Jenkins Global Security  
 * Click Manage Jenkins > click Configure Global Security  
 * Tick "Enable security" > Select Random for TCP port for JNLP slave agents  
 * Click on Agent protocols > Select all listed protocols  
 * In Access Control > Under Security Realm > select GitHub Authentication Plugin > enter the following:  
 >     GitHub Web URI:  https://github.com  
 >     GitHub API URI: https://api.github.com  
 >     Client ID: enter GitHub client ID here  
 >     Client Secret: enter GitHub secret here  
 >     OAuth Scope(s): read:org,user:email  
 * Under Authorization > select GitHub Committer Authorization Strategy  
 * Under GitHub Authorization Settings > enter the following:  
 >     Admin User names: enter names of administrators  
 >     Participant in Organization: enter organization name here  
 >     Use GitHub repository permissions: yes  
 * Click Save  
 **NOTE**: Client ID and secret needs to be generate beforehand on GitHub - OAuth applications  
 
#### Jenkins Configure System  
 * Click Manage Jenkins > click on Configure System  
 * Under GitHub Pull Request Builder > enter the following:  
 * GitHub Service API URL: https://api.github.com  
 * Shared Secret: enter secret here  
 * Credentials: select github service account here  
 * Under Cloud > click on Add a new cloud > select Amazon EC2 Container Service Cloud > enter the following:  
 >     Name: name_of_ecs_cloud
 >     Amazon ECS Credentials: select service account with access permission to ECS cluster  
 >     Amazon ECS Region Name: select region where the ECS cluster is located  
 >     Click Advanced > In Alternative Jenkins URL > enter "jenkins/domain/name" here  
 * Locate ECS slave templates > click Add > Enter the following:  
 >     Label: enter name which will be used to reference to this slave  
 >     Docker Image: enter docker image address retrieved from ECS
 >     Filesystem root: /home/jenkins  
 >     Memory: 1024  
 >     CPU units: 1  
 * Click save  
 **NOTE**: Amazon ECS credentials must be created beforehand on AWS Console - IAM section

<br />
### Configure GitHub and Amazon AWS credentials:
#### GitHub - Repository webhook  
 * Log into GitHub at https://github.com > navigate to organization/repository > click Settings  
 * Click Webhooks & services  
 * Click Add webhook > Select Jenkins (GitHub plugin) > Enter the following:  
   * Payload URL: jenkins/domain/name/ghprbhook/  
   * Content type: application/x-www-form-urlencoded  
   * Which events would you like to trigger this webhook? > select `Let me select individual events` and tick the following options:  
   >     Pull request  
   >     Member  
   >     Issue comment  
   * At the bottom, select ACTIVE  
 *Click Add webhook  

#### GitHub - OAuth Applications  
 * Log into GitHub at https://github.com > navigate to organization/name > click Settings  
 * Click on OAuth applications > Click on Register an application > Enter:  
 * Application name: enter application name here  
 * Homepage URL: jenkins/domain/name  
 * Application descriptions: some descriptions
 * Authorized callback URL: jenkins/domain/name or URL/where/user/access/Jenkins  

#### AWS - IAM  
 * Sign into AWS Console > click Services > click IAM  
 * Click Users > select the service user  
 * In `Security Credentials`, click on Create Access Key [use for AWS instance deployment]  
 * In Permissions tab, click Attach Policy > select the Policy you want to attach to this user account  
 * Click Attach Policy  


