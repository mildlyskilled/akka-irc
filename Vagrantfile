# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "hashicorp/precise32"
  config.vm.network "forwarded_port", guest: 1099, host: 8080
  config.vm.network :private_network, ip: "192.168.1.41"
  config.vm.synced_folder ".", "/srv/irc", :owner => 'vagrant', :group => 'vagrant'
end
