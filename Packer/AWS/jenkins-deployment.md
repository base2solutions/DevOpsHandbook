# Jenkins on AWS
A guide to deploying and configuring Jenkins on an AWS EC2 instance.

#### You Will Need:
* A url for your server
* [An AWS account](https://aws.amazon.com/)
* [A Github account](https://github.com/)

#### Utilized Files & Folders:
Files and folders from the base2solutions DevOps repository utilized in the Jenkins Packer deployment.

`Packer/AWS/baseJenkinsEC2.json` - [amazon-ebs Packer template](https://www.packer.io/docs/builders/amazon-ebs.html). Creates an [AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) on your AWS account from which an AWS EC2 instance will be launched.

`Packer/AWS/jenkins.sh` - Script that installs Java, Docker, Nginx, Git, and Jenkins.

`Packer/AWS/userNPermissions.sh` - Optional script that adds Vagrant as a user. Remove the line `"{{template_dir}}/scripts/userNPermissions.sh",` from `baseJenkinsEC2.json` if you'd like to exclude this script from the Packer AMI build.

`Packer/conf/jenkins.conf` - Jenkins nginx config file.

### Step 1: Deploy Jenkins via Packer

#### Packer AMI Build
 * Install [Packer](https://www.packer.io/downloads.html)  
 * Clone the DevOps repository:  
 `git clone https://github.com/base2solutions/DevOps.git`
 * Visit `Packer/conf/jenkins.conf` and replace the lines `<server url here>` with your server url.
 * Modify `baseJenkinsEc2.json` (the Packer template) as necessary.

 * Start Packer Build process:  
 `packer build path/to/baseJenkinsEC2.json`
 * Once the build process has been completed, sign into Amazon AWS Console

#### Launch EC2 instance from AMI
 * From the AWS Console, select EC2
 * On EC2:  
   * Under Images, click AMIs  
   * Select the previously created Jenkins AMI and click Launch > launch the AMI to create the Jenkins instance.  

 * For full list of available builders/provisioner/post-provisioner, check out [Packer Documentations](https://www.packer.io/docs). If what you are looking for is not found, it is likely available via Packer Plugins  

### Step 2: Configure Jenkins
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
 `include /etc/nginx/conf.d/*.conf`

#### Add letsencrypt certs  
 * Stop Nginx:  
 `sudo service nginx stop`
 * Generate letsencrypt certificate:  
 ```
 sudo wget https://dl.eff.org/certbot-auto  
 sudo chmod a+x certbot-auto  
 sudo ./certbot-auto certonly --standalone -d jenkins/domain/name
 ```  

#### Set Jenkins SSL certs in Jenkins config file:  
 * modify /etc/nginx/conf.d/jenkins.conf file as follow:  
   * Add:
   ```  
   ssl_certificate           /etc/letsencrypt/live/jenkins_domain_name/fullchain.pem;  
   ssl_certificate_key       /etc/letsencrypt/live/jenkins_domain_name/privkey.pem;  
   ssl on;  
   ```
   * Comment out:
   ```  
   #  ssl_certificate           /etc/nginx/cert.crt;  
   #  ssl_certificate_key       /etc/nginx/cert.key;  
   #  ssl_dhparam               /etc/nginx/ssl/dh2048.pem;  
   ```
 * Start Nginx:  
 `sudo service nginx start`  

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
 * In Access Control > Under Security Realm > select GitHub Authentication Plugin > enter the following   
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
 * Under Cloud > click on Add a new cloud > select Amazon EC2 Container Service Cloud > enter the following
 >    Name: name_of_ecs_cloud
 >    Amazon ECS Credentials: select service account with access permission to ECS cluster  
 >    Amazon ECS Region Name: select region where the ECS cluster is located  
 >    click Advanced > In Alternative Jenkins URL > enter "jenkins/domain/name" here

 * Locate ECS slave templates > click Add > Enter the following:  
 >    Label: enter name which will be used to reference to this slave  
 >    Docker Image: enter docker image address retrieved from ECS
 >    Filesystem root: /home/jenkins  
 >    Memory: 1024  
 >    CPU units: 1  

 * Click save  
 **NOTE**: Amazon ECS credentials must be created beforehand on AWS Console - IAM section

### Configure GitHub and Amazon AWS credentials:
#### GitHub - Repository webhook  
 * Log into GitHub at https://github.com > navigate to organization/repository > click Settings  
 * Click Webhooks & services  
 * Click Add webhook > Select Jenkins (GitHub plugin) > Enter the following:  
   * Payload URL: jenkins/domain/name/ghprbhook/  
   * Content type: application/x-www-form-urlencoded  
   * Which events would you like to trigger this webhook? > select `Let me select individual events` and tick the following options:
   ```  
   Pull request  
   Member  
   Issue comment  
   ```
   * At the bottom, select `ACTIVE` and Click `Add webhook`  

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

### Backup Jenkins Using AWS Lambda:
#### Overview  
 * To backup Jenkins using AWS Lambda, first we need to create a service role on AWS IAM. This will allow the service to handle EBS snapshot and perform instance backup and deletion of snapshots.  
 * Next we use AWS lambda to create a function, run via Python, to perform the required instance backup tasks. Then we attach the backup IAM role to this function to execute the specify tasks.  

#### Install and use AWS CLI:  
 * AWS CLI install - Windows  
   * Download the binary for Windows at - http://aws.amazon.com/cli/  
 * AWS CLI install - Mac and Linux (via pip)
   * Requires Python 2.6.5 or higher + PIP  
   * Install via `pip install awscli`
 * (Optional Manual Instructions)
   ```
   curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"  
   unzip awscli-bundle.zip  
   sudo ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws  
   ```
 * AWS Command structure:  
   `aws <command> <subcommand> [options and parameters]`

#### AWS - IAM permissions:  
 * Sign into Amazon > Click Services > Click IAM > Click Roles > Create New Role  
   * Step 1: Set Role Name  
     * In Role Name > enter "ebs-backup-worker"  
     * Click Next Step  
   * Step 2: Select Role Type  
     * Under AWS Service Role > select AWS Lambda  
     * Click Next Step  
   * Step 3: Establish Trust will be skipped automatically  
   * Step 4: Attach Policy  
     * Under Attach Policy > attach the following policies:  
     * AmazonEC2FullAccess  
     * Click Next Step  
   * Review and click Create Role  

#### AWS - Lambda - Jenkins backup worker:
 * Click Services > Click Lambda > Click Create a Lambda Function > Skip the Select Blueprint screen  
 * In Configure triggers > click the dotted box and select CloudWatch Events - Schedule  
   * Fill out the following fields:  
   * Rule name: enter your rule name here  
   * Rule description: enter your rule description here  
   * Schedule expression: `cron(00 02 ? * MON-FRI *)`
   * Tick the Enable trigger box  
   * Click Next  
 * In Configure function > fill out the following:  
   * Name: enter your function name  
   * Description: enter your function description here  
   * Runtime: Python 2.7  
   * Code entry type: Edit code inline  
   * In the text box below, copy and paste lines below:  

   ```
   python
   import boto3
   import collections
   import datetime

   ec = boto3.client('ec2')
   def lambda_handler(event, context):
   reservations = ec.describe_instances(
       Filters=[
           {'Name': 'tag-key', 'Values': ['Backup', 'backup']},
           {'Name': 'tag-value', 'Values': ['True','true']},
           ]
       ).get('Reservations', [])

   instances = sum([
       [i for i in instance['Instances']]
       for instance in reservations
       ], [])

   print "Found %d instances that need backing up" % len(instances)

   to_tag = collections.defaultdict(list)

   for instance in instances:
         try:
             retention_days = [
                 int(t.get('Value')) for t in instance['Tags']
                if t['Key'] == 'Retention'][0]
          except IndexError:
              retention_days = 5

          for dev in instance['BlockDeviceMappings']:
              if dev.get('Ebs', None) is None:
                  continue

              vol_id = dev['Ebs']['VolumeId']
              print "Found EBS volume %s on instance %s" % (vol_id, instance['InstanceId'])
              snap = ec.create_snapshot(VolumeId=vol_id)

              to_tag[retention_days].append(snap['SnapshotId'])

              print "Retaining snapshot %s of volume %s from instance %s for %d days" % (
                  snap['SnapshotId'], vol_id, instance['InstanceId'], retention_days)

          for retention_days in to_tag.keys():
              delete_date = datetime.date.today() + datetime.timedelta(days=retention_days)
              delete_fmt = delete_date.strftime('%Y-%m-%d')
              print "Will delete %d snapshots on %s" % (len(to_tag[retention_days]), delete_fmt)
              ec.create_tags(
                  Resources=to_tag[retention_days],
                  Tags=[{'Key': 'DeleteOn', 'Value': delete_fmt}]
                  )
   ```
   * Handler: default is `lambda_function.lambda_handler`  
   * Role: Choose an existing role  
   * Existing role: ebs-backup-worker  
   * Memory (MB): keep default or adjust as needed
   * Timeout: keep default or adjust as needed  
   * VPC: No VPC  
   * Click Next  
   * Review the function details and click Create function  
   * Finally, click Test to test the new function  

#### AWS - Lambda - Expired Jenkins snapshots remover:
 * Click Services > Click Lambda > Click Create a Lambda Function > Skip the Select Blueprint screen  
 * In Configure triggers > click the dotted box and select CloudWatch Events - Schedule  
   * Fill out the following fields:  
   * Rule name: enter your rule name here  
   * Rule description: enter rule description  
   * Schedule expression: `cron(00 20 ? * MON-FRI *)`  
   * Tick the Enable trigger box  
   * Click Next
 * In Configure function > fill out the following:  
   * Name: enter your function name here  
   * Description: enter your function description here  
   * Runtime: Python 2.7  
   * Code entry type: Edit code inline  
   * In the text box below, copy and paste lines below:  

   ```
   python
   import boto3
   import re
   import datetime

   ec = boto3.client('ec2')
   account_id = ['<aws_account_id_here>']
   def lambda_handler(event, context):
   delete_on = datetime.date.today().strftime('%Y-%m-%d')
       filters = [
           {'Name': 'tag-key', 'Values': ['DeleteOn']},
           {'Name': 'tag-value', 'Values': [delete_on]},
           ]

       snapshot_response = ec.describe_snapshots(OwnerIds=account_id, Filters=filters)

       for snap in snapshot_response['Snapshots']:
           print "Deleting snapshot %s" % snap['SnapshotId']
           ec.delete_snapshot(SnapshotId=snap['SnapshotId'])
   ```

   * Handler: default is `lambda_function.lambda_handler`  
   * Role: Choose an existing role  
   * Existing role: ebs-backup-worker  
   * Memory (MB): keep default or adjust as needed
   * Timeout: keep default or adjust as needed  
   * VPC: No VPC  
   * Click Next  
  * Review the function details and click Create function  
  * Finally, click Test to test the new function  
