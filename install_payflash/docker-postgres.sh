#!/bin/bash

## This script is for install postgres 11.2 on ubuntu 16.04 
## Need  install docker 
## Color rendering
Color_Off='\e[0m';
# ---- High Intensity ----
IRed='\e[0;91m';  IGreen='\e[0;92m';


install_docker(){

apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common -y

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -

sudo apt-key fingerprint 0EBFCD88

sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"

sudo apt-get update

sudo apt-get install docker-ce=18.06.3~ce~3-0~ubuntu -y

}

check_docker() {
RET=`docker -v &>/dev/null`

if [ $? -eq 0 ];then 
  echo -e  "$IGreen Docker_CE 18.06 already install and begin to install postgres $Color_Off"
  install_postgres
else 
  install_docker && install_postgres
fi

}

install_postgres(){
NAME=postgres
DATA_DIR=/pgdata
PORT=5432
PASSWD=Initial0
IMAGE_NAME=registry.kkops.cc/postgres:v1 

docker run -d --name $NAME \
-p $PORT:5432 \
-v $DATA_DIR:/var/lib/postgresql/data \
-e POSTGRES_PASSWORD=$PASSWD $IMAGE_NAME

sed -i '86s@127.0.0.1/32@0.0.0.0/0@' $DATA_DIR/pg_hba.conf
docker restart $NAME

if [ $? -eq 0 ]; then 

  echo -e "${IGreen}START postgres success $Color_Off"
else 
  echo -e "IRed START postgres Failed $Color_Off"      
fi
 }

check_docker
