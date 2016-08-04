# (AWS) Jenkins Deployment

<br />
### Deploy Jenkins via Packer:  
 * Install [Packer](https://www.packer.io/downloads.html)  
 * Log in to GitHub > navigate to [DualNFS Repo](https://github.com/base2solutions/777x.dual) > go into Packer directory and download the baseJenkinsECS.json template file.  
 * Download the script files used in by the template in the scripts folder.  
 * Start Packer Build process:  
 >     packer build baseJenkinsECS.json  
 * Once the build process has been completed, sign into [AWS Console](http://internal-base2.signin.aws.amazon.com/) > Click on Services and click EC2  
 * On EC2:  
   * Under Images, click AMIs  
   * Select the previously created Jenkins AMI and click Launch > launch the AMI to create the Jenkins instance.  

<br/>
### Configure Jenkins:  
#### Route 53  
 * On AWS, click on Services > click Route 53 > click on Hosted zones  
 * In Hosted zones:  
   * Click Create Hosted Zone and enter the following:  
     * Domain name: barbuild.base2d.com  
     * Comment: internal VPC for dual  
     * Type: Public Hosted Zone  
     * Click Create  
 * In Hosted zones, select the previously created zone > click Create Record Set  
     * In Create Record Set pane, enter:  
     * Name: barbuild.base2d.com  
     * Value: enter the private ip of the newly created Jenkins instance  
     * Click Create  

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
 >     sudo ./certbot-auto certonly --standalone -d barbuild.base2d.com  

#### Set Jenkins SSL certs in Jenkins config file:  
 * modify /etc/nginx/conf.d/jenkins.conf file as follow:  
   * Add:  
   >     ssl_certificate           /etc/letsencrypt/live/barbuild.base2d.com/fullchain.pem;  
   >     ssl_certificate_key       /etc/letsencrypt/live/barbuild.base2d.com/privkey.pem;  
   >     ssl on;  
   * Comment out:  
   >     #  ssl_certificate           /etc/nginx/cert.crt;  
   >     #  ssl_certificate_key       /etc/nginx/cert.key;  
   >     #  ssl_dhparam               /etc/nginx/ssl/dh2048.pem;  
 * Start Nginx:  
 >     sudo service nginx start  

#### Jenkins First Step   
 * Using a web browser, navigate to [https://barbuild.base2d.com.](https://barbuild.base2d.com])  
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
 >     Participant in Organization: base2solutions  
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
 >     Amazon ECS Region Name: select us-west-2  
 >     Click Advanced > In Alternative Jenkins URL > enter https://barbuild.base2d.com  
 * Locate ECS slave templates > click Add > Enter the following:  
 >     Label: buildslave  
 >     Docker Image: enter docker image address retrieved from ECS
 >     Filesystem root: /home/jenkins  
 >     Memory: 1024  
 >     CPU units: 1  
 * Click save  
 **NOTE**: Amazon ECS credentials must be created beforehand on AWS Console - IAM section

<br />
### Configure GitHub and Amazon AWS credentials:
#### GitHub - Repository webhook  
 * Log into GitHub at https://github.com > navigate to base2solutions/777x.dual repo > click Settings  
 * Click Webhooks & services  
 * Click Add webhook > Select Jenkins (GitHub plugin) > Enter the following:  
   * Payload URL: https://barbuild.base2d.com/ghprbhook/  
   * Content type: application/x-www-form-urlencoded  
   * Which events would you like to trigger this webhook? > select `Let me select individual events` and tick the following options:  
   >     Pull request  
   >     Member  
   >     Issue comment  
   * At the bottom, select ACTIVE  
 *Click Add webhook  

#### GitHub - OAuth Applications  
 * Log into GitHub at https://github.com > navigate to base2solutions > click Settings  
 * Click on OAuth applications > Click on Register an application > Enter:  
 * Application name: barbuild jenkins github auth  
 * Homepage URL: https://barbuild.base2s.com  
 * Application descriptions: some descriptions
 * Authorized callback URL: https://barbuild.base2s.com  

#### AWS - IAM  
 * Sign into AWS Console > click Services > click IAM  
 * Click Users > select the service user such as `service.dualnfs`  
 * In `Security Credentials`, click on Create Access Key [use for AWS instance deployment]  
 * In Permissions tab, click Attach Policy > select the Policy you want to attach to this user account  
