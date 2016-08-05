# (Virtualbox) Building Vagrant Box manually

<br />
### Converting Virtualbox VM to Vagrant box:
 * There are circumstance where we want to use an existing box, instead of building a new one. Below are steps to convert your existing virtual machine into a vagrant box:  
   * Query for the current vm list via `VBoxManage list vms`.  Below is a sample return:  
   >     "Ubuntu" {095ea74e-9b55-4777-b7fe-a6649683b830}
   * Finally, converting the vm into vagrant box via `vagrant package --base <ons_vm_uuid> --output <ons_vm_name>` such as:  
   >     "vagrant package --base 095ea74e-9b55-4777-b7fe-a6649683b830 --output Ubuntu.box

<br />
### (EXTRA) Uploading Vagrant box to Atlas:
 * At this point, you have two options to use the created Vagrant box:  
 >    1. Add the box to Vagrant via:  
 >       vagrant box add <vagrant_box_name> Ubuntu.box  
 <br />
 >    2. Add the box to Atlas as follow:  
 >       Log in to [Atlas](https://atlas.hashicorp.com/vagrant)  
 >       Under `BOXES` click on `base2dualnfs/ons`  
 >       On the left pane, click on New Version > this will lead to the New Box Version screen  
 >       Enter the new box Version and fill in the Description box > click `Create version`  
 >       Next, click on `Create new provider` > this will lead New Box Provider screen  
 >       Under Provider section, select `virtualbox`  
 >       Then select `Upload` > click `Continue to upload` > this will lead to Edit Box Provider screen    
 >       Click `Browse` and select the box file created on your laptop  
 >       Wait for your box to be uploaded > click Finish when done  

