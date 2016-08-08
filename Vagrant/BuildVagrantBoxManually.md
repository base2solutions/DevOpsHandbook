# (Virtualbox) Building Vagrant Box manually

<br />
### Converting Virtualbox VM to Vagrant box:
 * There are circumstance where we want to use an existing box, instead of building a new one. Below are steps to convert your existing virtual machine into a vagrant box:  
   * Query for the current vm list via `VBoxManage list vms`.  Below is a sample return:  
   >     "Ubuntu" {095ea74e-9b55-4777-b7fe-a6649683b830}
   * Then convert the vm into vagrant box via `vagrant package --base <ons_vm_uuid> --output <ons_vm_name>`:  
   >     "vagrant package --base 095ea74e-9b55-4777-b7fe-a6649683b830 --output Ubuntu.box  
   * Finally, add the box to Vagrant via:  
   >       vagrant box add <vagrant_box_name> Ubuntu.box  

