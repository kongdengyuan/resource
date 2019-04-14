#!/bin/bash
set -ex 
## Color renderin

Color_Off='\e[0m';
# ---- High Intensity ----
Red='\e[0;91m';  Green='\e[0;92m';

SCRIPT_DIR=$(cd "$(dirname $0)"; pwd)
CODE_DIR=/deploy/PayFlash
SQL="psql -h localhost -U postgres"

## Install dependency package

curl -sL https://deb.nodesource.com/setup_11.x | sudo -E bash -
apt-get install -y nodejs  maven  openjdk-8-jdk build-essential
npm install -g @angular/cli  ## for angular ui  

checkRetVal () {
  if [ $? -ne 0 ]; then
      exit $?
  else 
  
    echo -e "${Green}Service install success $Color_Off " &&  sleep 1 
 
  fi
}

#Install x4

$SQL -c 'create schema "BYD"'

cd $CODE_DIR/payment-manager 

cp $SCRIPT_DIR/HtmlPane.js /deploy/x4/ui/client/resources/sap/b/controls/panes

./setup_x4_service.sh 

checkRetVal

sed -i '35s#start#start >/var/log/payment/x4.log \&#' start_x4_service.sh

mkdir /var/log/payment

./start_x4_service.sh 

checkRetVal

echo -e "${red}Please go to brower open x4 to add 6 ilab button $Color_Off "
sleep 100

# Install payment service
# Copy template java
#cp $SCRIPT_DIR/PaymentServiceImpl.java $CODE_DIR/payment-service/src/main/java/com/sap/sme/payment/service/impl

cp $SCRIPT_DIR/EmailServiceImpl.java  $CODE_DIR/payment-service/src/main/java/com/sap/sme/payment/service/impl

cd $CODE_DIR/payment-service

$SQL  -c 'drop table "BYD"."pfPayment"'

$SQL   --set ON_ERROR_STOP=ON -f  dbInit_PostgreSQL.sql 

mvn package 

checkRetVal

#Install payment frontend

cd  $CODE_DIR/payment-frontend

npm install 

$SQL -c "\copy \"BYD\".\"ilab/paymentmgmt/PaymentAccount\" from '$SCRIPT_DIR/pay_account_data'"

checkRetVal



