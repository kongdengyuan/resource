#!/bin/bash
set -ex 
## check root user 
[[ $EUID -ne 0 ]] && echo "Error: This script must be run as root!" && exit 1

## Color renderin
Color_Off='\e[0m';
# ---- High Intensity ----
Red='\e[0;91m';  Green='\e[0;92m';


## ENV Settings
PARA=$1
SCRIPT_DIR=$(cd "$(dirname $0)"; pwd)
CODE_DIR=/deploy
SQL="psql -h localhost -U postgres"

## uninstall service
uninstall_service(){

$SCRIPT_DIR/pay-frontend.sh stop
$SCRIPT_DIR/pay-service.sh stop
$SCRIPT_DIR/pay-manager.sh stop
rm -r /deploy/PayFlash
$SCRIPT_DIR/docker-postgres.sh uninstall

}

## uninstall all of service
if [ $PARA = "uninstall" ];then
    uninstall_service && exit 0 
fi 

## Install psotgres
$SCRIPT_DIR/docker-postgres.sh 

## Install dependency package
curl -sL https://deb.nodesource.com/setup_11.x | sudo -E bash -
apt-get install -y nodejs  maven  openjdk-8-jdk build-essential unzip

## install angular cli
! npm list -g @angular/cli | grep @angular &>/dev/null  &&  npm install -g @angular/cli 

## Pull code 
[ ! -d $CODE_DIR  ] && mkdir $CODE_DIR
cd $CODE_DIR 
[ ! -d PayFlash ] &&  git clone https://github.wdf.sap.corp/ilab/PayFlash.git
#[ ! -d x4 ] && git clone https://github.wdf.sap.corp/BIG/x4.git 

checkRetVal () {
if [ $? -ne 0 ]; then
      exit $?
  else 

echo -e "${Green}Service install success $Color_Off " &&  sleep 1 
 
fi
}

#Install x4

cd $CODE_DIR/PayFlash/payment-manager 

cp $SCRIPT_DIR/HtmlPane.js /deploy/x4/ui/client/resources/sap/b/controls/panes

sed -i  '69a\export HTTPS_PORT=3999' $CODE_DIR/PayFlash/payment-manager/setup_x4_service.sh

./setup_x4_service.sh 

checkRetVal

sed -i '35s#start#start >/var/log/payment/x4.log \&#' start_x4_service.sh

[ ! -d /var/log/payment ] && mkdir /var/log/payment

./start_x4_service.sh 

checkRetVal

echo -e "${red}Please go to brower open x4 to add 8 ilab button $Color_Off "

sleep 90

# Install payment service
# Copy template java

cp $SCRIPT_DIR/PaymentServiceImpl.java $CODE_DIR/PayFlash/payment-service/src/main/java/com/sap/sme/payment/service/impl
cp $SCRIPT_DIR/EmailServiceImpl.java  $CODE_DIR/PayFlash/payment-service/src/main/java/com/sap/sme/payment/service/impl

cd $CODE_DIR/PayFlash/payment-service

$SQL  -c 'drop table "BYD"."pfPayment"'
$SQL  -c 'drop table "BYD"."B1Orders"'
$SQL  -c 'drop table "BYD"."B1DocumentLines"'

$SQL   --set ON_ERROR_STOP=ON -f  dbInit_PostgreSQL.sql
$SQL   --set ON_ERROR_STOP=ON -f mock_orderData.sql
mvn package 

checkRetVal

# Install payment frontend

cp  $SCRIPT_DIR/index.html $CODE_DIR/PayFlash/payment-frontend/src 

cd  $CODE_DIR/PayFlash/payment-frontend

npm install 

$SQL -c "\copy \"BYD\".\"ilab/paymentmgmt/PaymentAccount\" from '$SCRIPT_DIR/pay_account_data'"

checkRetVal

# start service 

$SCRIPT_DIR/pay-frontend.sh start
$SCRIPT_DIR/pay-service.sh start 

