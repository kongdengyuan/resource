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
# apt-cache madison docker-ce  # find docker-ce version 
sudo apt-get install docker-ce=5:18.09.5~3-0~ubuntu-xenial -y

}

check_docker() {
RET=`docker -v | grep "18.09" &>/dev/null`

if [ $? -eq 0 ];then 
  echo -e  "${IGreen}Docker_CE 18.09 already install and begin to install postgres $Color_Off"
  install_postgres
else 
  install_docker && install_postgres && install_postgres_client
fi

}

install_postgres(){
NAME=postgres
DATA_DIR=/pgdata
PORT=5432
PASSWD=abcd1234
#IMAGE_NAME=registry.kkops.cc/postgres:v1 
IMAGE_NAME=postgres:11.2

docker run -d --name $NAME \
-p $PORT:5432 \
-v $DATA_DIR:/var/lib/postgresql/data \
-e POSTGRES_PASSWORD=$PASSWD $IMAGE_NAME

sleep 2

sed -i '86s@127.0.0.1/32@0.0.0.0/0@' $DATA_DIR/pg_hba.conf
docker restart $NAME

if [ $? -eq 0 ]; then 

  echo -e "${IGreen}START postgres success $Color_Off"
else 
  echo -e "IRed START postgres Failed $Color_Off"      
fi
 }
install_postgres_client(){
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
apt-get update
apt-get install postgresql-client-11 -y

if [ $? -eq 0 ]; then

  echo -e "${IGreen}Install postgres client success $Color_Off"
else
  echo -e "I{Red}Install postgres client Failed $Color_Off"
fi

}
check_docker
