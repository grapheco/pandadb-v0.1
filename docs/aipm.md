# aipm-web
This is the aipm-web project.

## Requirement
```
Centos 7 or later relese edition.
Docker 19.03.12 or later
```

## Deployment
```
1. docker (if needed)
sudo yum install -y yum-utils  device-mapper-persistent-data  lvm2
sudo yum-config-manager  --add-repo   https://download.docker.com/linux/centos/docker-ce.repo
yum install https://download.docker.com/linux/centos/7/x86_64/stable/Packages/containerd.io-1.2.6-3.3.el7.x86_64.rpm
sudo yum install docker-ce docker-ce-cli

2. pull the image
docker pull airzihao/aipm:aipm_web0.1  #The cost time depends on your network env, may takes several minutes.
docker run -ditp 8081:8081 airzihao/aipm:aipm_web0.1 /bin/bash
docker exec -it $container_id /bin/bash

3. exec in the container
cd /home/aipm
nohup python3 manage.py runserver 0.0.0.0:8081
```

## Developer Manual
### 1. Open the fastest mirror.(Optional on centos8)
```
vi /etc/dnf/dnf.conf
fastestmirror=True
sudo dnf clean all
sudo dnf makecache
```


## 2. Install the docker

```
sudo yum install -y yum-utils  device-mapper-persistent-data  lvm2
sudo yum-config-manager  --add-repo   https://download.docker.com/linux/centos/docker-ce.repo
yum install https://download.docker.com/linux/centos/7/x86_64/stable/Packages/containerd.io-1.2.6-3.3.el7.x86_64.rpm
sudo yum install docker-ce docker-ce-cli
```

```
# Start docker service and confirm the version.
sudo systemctl start docker
docker --version

# Start the docker service on system start.
systemctl enable docker.service
systemctl start docker.service
```


## 3. Pull the image
```
docker pull airzihao/aipm:aipm_web0.1 

# Import the image.
docker import aipm_web_01.tar
# Create the container.
docker  run  -ditp 8081:8081  --name=aipmv0.1_base  $<image-id>  /bin/bash
# Enter the container.
docker exec -it aipmv0.1_base /bin/bash

cd /home/aipm-web
nohup python3 manage.py runserver 0.0.0.0:8081 &
```

## Create a new image (If needed.)
The following operations are evaluated inner the CentOS7-base image.
```
# Update yum source
mv /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.bak
cd /etc/yum.repos.d/
curl -O http://mirrors.163.com/.help/CentOS7-Base-163.repo
mv CentOS7-Base-163.repo CentOS-Base.repo
yum clean all
yum makecache

# Insall the python
yum -y install wget
wget https://www.python.org/ftp/python/3.6.5/Python-3.6.5.tgz
mv Python-3.6.5.tgz /usr/local
cd /usr/local
tar -xvzf Python-3.6.5.tgz
cd Python-3.6.5
yum install openssl-devel bzip2-devel expat-devel gdbm-devel readline-devel sqlite-devel  -y
yum -y install gcc automake autoconf libtool make
./configure   
make
make install

# Update the Pip source
mkdir ~/.pip
vi ~/.pip/pip.conf
[global]
index-url = https://mirrors.aliyun.com/pypi/simple

yum install -y git

git clone https://github.com/cas-bigdatalab/aipm-web.git
# Config the aipm-web:
# Note: Do not adjust the order of the following commands!

pip3 install django==2.1.8
pip3 install cmake
yum install gcc-c++ -y
yum -y install boost-devel
pip3 install boost
pip3 install dlib==19.6.1
pip3 install numpy
pip3 install Pillow
pip3 install hyperlpr==0.0.1
pip3 install keras
pip3 install tensorflow
pip3 install matplotlib
pip3 install python_speech_features
pip3 install jieba


yum install libSM-1.2.2-2.el7.x86_64 --setopt=protected_multilib=false
yum install libXrender.x86_64 -y
yum install libXext-1.3.3-3.el7.x86_64 -y
```