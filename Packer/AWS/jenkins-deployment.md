# Jenkins on AWS
A guide to deploying and configuring Jenkins with GitHub authentication on an AWS EC2 instance.

To begin you need a url for your server, an [AWS](https://aws.amazon.com/) account, and a [Github ](https://github.com/) account.

#### Utilized Files & Folders:
Files and folders from the base2solutions DevOps repository utilized in the Jenkins Packer deployment.

`Packer/AWS/baseJenkinsEC2.json` - The [Packer Template](https://www.packer.io/docs/templates/introduction.html). Packer templates are JSON files that configure Packer in order to create machine images. This [amazon-ebs](https://www.packer.io/docs/builders/amazon-ebs.html) Packer template creates an [AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) on your AWS account from which an AWS EC2 instance will be launched.

`Packer/AWS/jenkins.sh` - Script that installs Java, Docker, Nginx, Git, and Jenkins.

`Packer/AWS/userNPermissions.sh` - Optional script that adds Vagrant as a user. Remove the line `"{{template_dir}}/scripts/userNPermissions.sh",` from `baseJenkinsEC2.json` if you'd like to exclude this script from the Packer AMI build.

`Packer/conf/jenkins.conf` - Jenkins nginx config file.
### Step 1: Set Up with AWS CLI

1. Create & Save your AWS access key ID and secret access key. [Instructions available here.](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-set-up.html)

2. Create a separate service user (if nonexistent)
 * `Services > Users > Create New User`
 * Give service user a name and hit `Create`
 * Select `Download Security Credentials` and `close`
 * Move service user credentials to a safe place

3. Add Permissions to service user
 * `Users > service user > Permissions > Attach Policy`
 * Attach `AmazonEC2FullAccess` and `AmazonEC2ContainerServiceFullAccess`

4. Install the AWS CLI (2 options)
 * [AWS CLI Global Installation](http://docs.aws.amazon.com/cli/latest/userguide/installing.html)
 * Install AWS CLI in a Python virtual environment (recommended):
    * Install pip (python package manager)
    * Do `sudo pip install virtualenv`
    * Create a `VIRTUAL_ENVS` directory hold your Python virtual environments
    * Change directory into `VIRTUAL_ENVS` and create a new virtual environment with `virtualenv jenkins-env`. This will create a directory called `jenkins-env`within `VIRTUAL_ENVS`.
    * Change directory into `jenkins-env` and do `source bin/activate` to activate your virtual environment.
    * In your virtual environment, do `pip install awscli`
    * Move on to the `Configure AWS CLI` step.
    * `virtualenv` tips:
      * To exit type `deactivate` in your terminal.
      * To reactivate change directory into `jenkins-env` and do `source bin/activate`
      * More info on `virtualenv` [here](https://virtualenv.pypa.io/en/stable/).

5. Configure the AWS CLI.
   * Do `aws configure`. If you are using `virtualenv`, do `aws configure` from within the virtual environment where `aws cli` was installed.
   * input your (not the service user) AWS Access Key ID
   * input your AWS Secret Access Key
   * input your default region name, e.g. `us-west-2`
   * input `json` as the Default output format
   * [more info here.](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-quick-configuration)

6. Create an Amazon EC2 Key Pair.
  * change directory to `/Users/<user name>/.ssh/`
  * do `aws ec2 create-key-pair --key-name <AWS key pair name> --query "KeyMaterial" --output text > <AWS key pair name>.pem`
  * do `chmod 700 <AWS key pair name>.pem`

For more information on the aws-cli Python package checkout their [GitHub repository](https://github.com/aws/aws-cli).

### Step 2: Create AWS Variables for Packer

1. Create a [VPC](http://docs.aws.amazon.com/AmazonVPC/latest/UserGuide/getting-started-create-vpc.html)
2. Create a Subnet
  * From the AWS Console navigate to `Services > VPC > Subnets > Create Subnet`
  * Associate it with the VPC you created
  * Select your Availability Zone
  * Input a Subnet CIDR block with the same IP as your VPC but with a different netmask.
3. Create a [Security Group](http://docs.aws.amazon.com/AmazonVPC/latest/UserGuide/getting-started-create-security-group.html) and configure with the following Inbound & Outbound:

Inbound
```
Type            Protocol            Port Range          Source
HTTP              TCP                   80             0.0.0.0/0
All traffic       All                   All            <this security group>
All traffic       All                   All             <your vpc CIDR>
SSH               TCP                   22               0.0.0.0/0
HTTPS             TCP                   443              0.0.0.0/0
```

Outbound
```
Type            Protocol            Port Range          Destination
All traffic         All                All               0.0.0.0/0
```

### Step 3: Deploy Jenkins via Packer

 Install [Packer](https://www.packer.io/downloads.html)

 Clone the base2solutions DevOps repository:
`git clone https://github.com/base2solutions/DevOps.git`

 Visit `Packer/conf/jenkins.conf` and replace the lines `<server url here>` with your server url.

#### Modify the Packer Template

  The `baseJenkinsEc2.json` Packer template is composed of:
* [variables](https://www.packer.io/docs/templates/user-variables.html)
* [builders](https://www.packer.io/docs/templates/builders.html)
* [provisioners](https://www.packer.io/docs/templates/provisioners.html)

Variables allow us the ability to keep secret tokens and other configuration data out of our Packer template.

##### Variable Guide
Variables used in the `baseJenkinsEC2.json` template:
```
"variables": {                      // variable descriptions below
    "ssh_username": "",             // create a username to ssh into your EC2 instance with.
    "aws_instance_type": "",        // EC2 instance type e.g. t2.nano, t2.micro, etc.
    "aws_region": "",               // location where your EC2 is hosted e.g. us-east-1, eu-central-1, etc.
"aws_access_key": "",               // aws access key stored in ~/.aws/credentials
"aws_secret_key": "",               // aws secret key stored in ~/.aws/credentials
    "aws_source_ami": "",           // Amazon Machine Image (AMI) reference ID e.g. input ami-7172b611 for Amazon Linux, ami-775e4f16 for Red Hat Enterprise Linux, etc.
    "aws_subnet_id": "",            // Subnet ID of the Subnet we created earlier e.g. subnet-lskdjf99
    "aws_vpc_id": "",               // VPC ID of the VPC created earlier e.g. vpc-fjlk239333
    "aws_security_group_id":"",     // Security Group ID created earlier e.g. sg-lksdjfkl88
    "aws_ami_name": "",             // create a name for your AMI
    "aws_ami_users": ""             // numeric part of your AWS User ARN. visit AWS > IAM > Users > your username > User ARN:  arn:aws:iam::<the digits here>::user/user-name
}
```

##### Variables and Builders Parameters

Variables are used for builders parameters in the Packer template.
example:

```
{
  "variables": {
    "aws_instance_type": "",
    "aws_region": "",
    "aws_access_key": "",
    "aws_secret_key": ""
  },

  "builders": [{
    "access_key": "{{user `aws_access_key`}}",
    "secret_key": "{{user `aws_secret_key`}}",
  	"instance_type": "{{user `aws_instance_type`}}",
  	"region": "{{user `aws_region`}}"
  }]
}
```

##### Builders Parameter Options
3 Options for defining variables / configuring builders parameters.
Choose the option that makes sense for your project.
Option 3. would not be appropriate for templates stored in public repositories but is fine for private repositories.

After choosing your method, reference the `Variable Guide` to help identify your variables/builders parameters.

Option 1. Set variables from the command line during packer build:
```
packer build \
-var 'aws_region=us-west-1' \
-var 'aws_instance_type=t2.micro' \
baseJenkinsEC2.json
```

Option 2. Set variables in an external JSON file:
```
{
  "aws_access_key": "foo",
  "aws_secret_key": "bar",
  "aws_instance_type": "t2.micro"
}
```
Then do `packer build -var-file=variables.json baseJenkinsEC2.json`

Option 3. Replace with real values.
 * Delete all variables except for `aws_access_key` and `aws_secret_key`
 * Replace all `{{ user aws_stuff_here }}` template variable references except the `{{ user aws_access_key }}` and `{{ user aws_secret_key }}`:

```
 {
   "variables": {
     "aws_access_key": "",
     "aws_secret_key": ""
   },

   "builders": [{
     "type": "amazon-ebs",
     "access_key": "{{user `aws_access_key`}}",
     "secret_key": "{{user `aws_secret_key`}}",
   	"instance_type": "t2.micro",
   	"region": "us-west-1"
   }]
 }
```

Then do `packer build path/to/baseJenkinsEC2.json`
#### Packer AMI Build
 * Identify Variable & Builders Parameters method and gather necessary parameters.
 * Start Packer Build process with your preferred method:

 `packer build \ -var 'aws_region=us-west-1' \ -var ...etc.` (Option 1)

 `packer build -var-file=variables.json path/to/baseJenkinsEC2.json` (Option 2)

 `packer build path/to/baseJenkinsEC2.json` (Option 3)
 * Once the build process has been completed, sign into Amazon AWS Console

##### Troubleshooting

If you encounter this error during a  `packer build`:
```
Build 'amazon-ebs' errored: Script exited with non-zero exit status: 1

==> Some builds didn't complete successfully and had errors:
--> amazon-ebs: Script exited with non-zero exit status: 1

==> Builds finished but no artifacts were created.
```

Export your AWS Access Key and Access Key ID like so from your terminal:

`export AWS_ACCESS_KEY_ID=<access key id here>`

`export AWS_SECRET_ACCESS_KEY=<secret key here>`

Export these keys and do `packer build` again.

#### Launch EC2 instance from AMI
 * From the AWS Console, select `EC2`
 * Under Images, select `AMIs`
 * Select the newly created AMI and hit `Launch`:
    * "Choose an Instance Type"
      * Select your instance type. `t2.micro` is sufficient.
    * "Configure Instance Details"
      * Select the VPC used in the Packer template.
      * Under `Auto-assign Public IP` select `Enable`
    * "Add Storage"
      * No modifications needed if you specified the storage type in the Packer template.
    * "Tag Instance"
      * Give your instance a name.
    * "Configure Security Group"
      * Select the Security Group used in the Packer template.
  * Review and Launch.
  * Select your EC2 Key Pair created in `Step 1: Set Up with AWS CLI`

### Step 4: Configure Route 53
Use [Amazon Route 53](https://aws.amazon.com/route53/) to create Public and Private Hosted Zones. Public Hosted Zone will provide DNS name resolution for your Jenkins when it is accessed via the internet. Private Hosted Zone will provide DNS name resolution for Jenkins when it is accessed by other servers within the same Amazon VPC.

 * On AWS, select Services > Route 53 > Hosted zones  
 * Follow these steps to add a new Domain & Record Sets:
   * Select `Create Hosted Zone` and create a Public Hosted Zone:  
     * Domain name: e.g. `myjenkinsdomainname.com`
     * Comment: some/comment/here  
     * Type: Public Hosted Zone  
     * Click Create  
   * Select `Create Record Set`   
     * Name: e.g. `jenkins1.myjenkinsdomainname.com`
     * Value: enter the public ip of the Jenkins EC2 instance.
     * Select `Create`  
   * Select `Create Hosted Zone` and create a Private Hosted Zone:  
     * Domain name: e.g. `myjenkinsdomainname.com`
     * Comment: some/comment/here   
     * Type: Private Hosted Zone for Amazon VPC  
     * VPC ID: choose the Amazon VPC that you want to associated with the hosted zone  
     * Select `Create`  
   * In Hosted zones, select the previously created private zone > click Create Record Set  
     * In Create Record Set pane, enter:  
     * Name: e.g. `jenkins1.myjenkinsdomainname.com`  
     * Value: enter the private ip of the the Jenkins EC2 instance.  
     * Select `Create`

* Follow these steps to modify existing Route 53 configurations:
    * Replace old Public & Private IP addresses with that of your Jenkins EC2 instance.

### Step 5: Configure GitHub
#### GitHub - Repository webhook  
 * your organization > repository > Settings  
 * Select Webhooks & services  
 * Select `Add webhook`:  
   * Payload URL: `<Jenkins record set domain>/ghprbhook/`  
   * Content type: application/x-www-form-urlencoded  
   * Which events would you like to trigger this webhook? > select `Let me select individual events` and tick the following options:
   ```  
   Pull request  
   Member  
   Issue comment  
   ```
   * At the bottom, select `ACTIVE` and Click `Add webhook`  

#### GitHub - OAuth Applications  
 * Select your user icon > settings
 * Select OAuth applications > Register a new application:  
 * Application name: enter application name here  
 * Homepage URL: <Jenkins record set domain>  
 * Application descriptions: some descriptions
 * Authorized callback URL: `https://<Jenkins record set domain>/securityRealm/finishLogin` or URL/where/user/access/Jenkins  

### Step 6: Configure Jenkins
#### Nginx
 * SSH into the newly created Jenkins instance

 `ssh -i /Users/<your user name>/.ssh/<your-AWS-Key-Pair.pem> <user name defined in the Packer template>@<the EC2 instance public DNS>`  
 * Navigate to /etc/nginx/nginx.conf > Uncomment this line:
 `include /etc/nginx/conf.d/*.conf`

#### Add letsencrypt certs  
 * Stop Nginx:  
 `sudo service nginx stop`
 * Generate letsencrypt certificate:  

 `sudo wget https://dl.eff.org/certbot-auto`

 `sudo chmod a+x certbot-auto`

 `sudo ./certbot-auto certonly --standalone -d <Jenkins record set domain>`

 * Start Nginx:  
 `sudo service nginx start`  

#### Initial Jenkins Login   
 * Using a web browser, navigate to the Jenkins instance url.
 * While ssh'd in to the Jenkins instance, do `sudo su` and `cat /var/lib/jenkins/secrets/initialAdminPassword`. The output is the initial Jenkins password.
 * Copy the output and use it to log in to Jenkins.
 * Log in using the password created in `/var/lib/jenkins/secrets/initialAdminPassword`  
 * Install most recommended plugins  
 * Create a local admin account  

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
 * Select `Restart Jenkins when installation is complete and no jobs are running`.  

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
 >    click Advanced > In Alternative Jenkins URL > enter "<Jenkins record set domain>" here

 * Locate ECS slave templates > click Add > Enter the following:  
 >    Label: enter name which will be used to reference to this slave  
 >    Docker Image: enter docker image address retrieved from ECS
 >    Filesystem root: /home/jenkins  
 >    Memory: 1024  
 >    CPU units: 1  

 * Click save  
 **NOTE**: Amazon ECS credentials must be created beforehand on AWS Console - IAM section


### Backup Jenkins Using AWS Lambda:
#### Overview  
 * To backup Jenkins using AWS Lambda, first we need to create a service role on AWS IAM. This will allow the service to handle EBS snapshot and perform instance backup and deletion of snapshots.  
 * Next we use AWS lambda to create a function, run via Python, to perform the required instance backup tasks. Then we attach the backup IAM role to this function to execute the specify tasks.  

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
